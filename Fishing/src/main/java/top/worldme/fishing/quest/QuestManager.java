package top.worldme.fishing.quest;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import top.worldme.fishing.config.FishingConfig;
import top.worldme.fishing.config.FishingConfig.QuestFish;
import top.worldme.fishing.config.FishingConfig.Reward;
import top.worldme.fishing.data.PlayerQuestData;
import top.worldme.fishing.util.QuestKeys;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class QuestManager {

    private final FishingConfig config;
    private final PlayerQuestData data;
    private final QuestKeys keys;
    private final Random random = new Random();
    private final DateTimeFormatter cycleFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public QuestManager(FishingConfig config, PlayerQuestData data, QuestKeys keys) {
        this.config = config;
        this.data = data;
        this.keys = keys;
    }

    /**
     * 获取当前任务周期键（按 config 中 cycle-time 切换）
     */
    public String getCurrentCycleKey() {
        String cycleTimeStr = config.getCycleTime();
        LocalTime cycleTime;
        try {
            cycleTime = LocalTime.parse(cycleTimeStr);
        } catch (Exception e) {
            cycleTime = LocalTime.of(4, 30);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        if (now.toLocalTime().isBefore(cycleTime)) {
            date = date.minusDays(1);
        }
        return date.format(cycleFormatter);
    }

    /**
     * 确保玩家当前周期已分配任务；如周期已切换则重新分配
     */
    public QuestState ensureQuest(Player player) {
        UUID uuid = player.getUniqueId();
        String currentCycle = getCurrentCycleKey();
        String storedCycle = data.getCycle(uuid);

        if (!currentCycle.equals(storedCycle)) {
            assignNewQuest(uuid, currentCycle);
        }

        String questLootId = data.getQuestLootId(uuid);
        QuestFish questFish = config.getQuestFishByLootId(questLootId);
        if (questFish == null) {
            assignNewQuest(uuid, currentCycle);
            questLootId = data.getQuestLootId(uuid);
            questFish = config.getQuestFishByLootId(questLootId);
        }

        return new QuestState(
                currentCycle,
                questFish,
                data.isCaught(uuid),
                data.isCompleted(uuid)
        );
    }

    public QuestState getState(Player player) {
        UUID uuid = player.getUniqueId();
        String currentCycle = getCurrentCycleKey();
        String storedCycle = data.getCycle(uuid);
        String questLootId = data.getQuestLootId(uuid);
        QuestFish questFish = config.getQuestFishByLootId(questLootId);

        boolean valid = currentCycle.equals(storedCycle) && questFish != null;
        return new QuestState(
                currentCycle,
                valid ? questFish : null,
                valid && data.isCaught(uuid),
                valid && data.isCompleted(uuid)
        );
    }

    private void assignNewQuest(UUID uuid, String cycle) {
        List<QuestFish> pool = config.getQuestFishes();
        if (pool.isEmpty()) {
            data.clear(uuid);
            data.save();
            return;
        }

        int totalWeight = 0;
        for (QuestFish fish : pool) {
            totalWeight += Math.max(0, fish.weight());
        }

        QuestFish selected;
        if (totalWeight <= 0) {
            selected = pool.get(0);
        } else {
            int roll = random.nextInt(totalWeight);
            int current = 0;
            QuestFish temp = pool.get(0);
            for (QuestFish fish : pool) {
                current += Math.max(0, fish.weight());
                if (roll < current) {
                    temp = fish;
                    break;
                }
            }
            selected = temp;
        }

        data.setCycle(uuid, cycle);
        data.setQuestLootId(uuid, selected.cfLootId());
        data.setCaught(uuid, false);
        data.setCompleted(uuid, false);
        data.save();
    }

    /**
     * 供 CustomFishing requirement 调用：该 loot 对玩家是否处于可钓状态
     */
    public boolean isQuestActiveForLoot(Player player, String lootId) {
        QuestState state = getState(player);
        if (state.questFish() == null) {
            return false;
        }
        return state.questFish().cfLootId().equals(lootId)
                && !state.caught()
                && !state.completed();
    }

    public void markCaught(Player player) {
        data.setCaught(player.getUniqueId(), true);
        data.save();
    }

    /**
     * 给背包中尚未绑定的任务鱼打上 PDC（用于 CustomFishing 直接进背包等场景）
     */
    public void tagInventoryQuestFish(Player player, QuestFish questFish) {
        String ownerUuid = player.getUniqueId().toString();
        String cycle = getCurrentCycleKey();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                continue;
            }
            if (!matchesQuestItem(item, questFish.ceItemId())) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(keys.owner, PersistentDataType.STRING) || pdc.has(keys.cycle, PersistentDataType.STRING)) {
                continue;
            }
            pdc.set(keys.owner, PersistentDataType.STRING, ownerUuid);
            pdc.set(keys.cycle, PersistentDataType.STRING, cycle);
            item.setItemMeta(meta);
        }
    }

    public void markCompleted(Player player) {
        data.setCompleted(player.getUniqueId(), true);
        data.save();
    }

    public boolean canComplete(Player player) {
        QuestState state = getState(player);
        return state.questFish() != null
                && state.caught()
                && !state.completed();
    }

    /**
     * 在玩家背包中寻找一条有效的任务鱼（含正确 PDC）
     */
    public ItemStack findValidQuestFish(Player player) {
        QuestState state = getState(player);
        if (state.questFish() == null) {
            return null;
        }
        String expectedCeItemId = state.questFish().ceItemId();
        String currentCycle = state.cycle();
        String ownerUuid = player.getUniqueId().toString();

        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                continue;
            }
            if (!matchesQuestItem(item, expectedCeItemId)) {
                continue;
            }
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            String itemOwner = pdc.get(keys.owner, PersistentDataType.STRING);
            String itemCycle = pdc.get(keys.cycle, PersistentDataType.STRING);
            if (ownerUuid.equals(itemOwner) && currentCycle.equals(itemCycle)) {
                return item;
            }
        }
        return null;
    }

    public boolean matchesQuestItem(ItemStack item, String expectedCeItemId) {
        if (!CraftEngineItems.isCustomItem(item)) {
            return false;
        }
        Key key = CraftEngineItems.getCustomItemId(item);
        return key != null && expectedCeItemId.equalsIgnoreCase(key.asString());
    }

    /**
     * 尝试完成玩家任务，返回是否成功
     */
    public boolean tryComplete(Player player) {
        if (!canComplete(player)) {
            return false;
        }
        ItemStack questFish = findValidQuestFish(player);
        if (questFish == null) {
            return false;
        }

        // 扣除一条任务鱼
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item == questFish) {
                if (item.getAmount() <= 1) {
                    contents[i] = null;
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                break;
            }
        }
        inventory.setStorageContents(contents);

        // 累计完成次数 +1，并根据次数选取奖池发放奖励
        UUID uuid = player.getUniqueId();
        int newTotal = data.getTotalCompleted(uuid) + 1;
        data.setTotalCompleted(uuid, newTotal);
        for (Reward reward : config.getRewardsForCompletion(newTotal)) {
            giveReward(player, reward);
        }

        markCompleted(player);
        return true;
    }

    private void giveReward(Player player, Reward reward) {
        if (reward.isCommand()) {
            String command = reward.command()
                    .replace("<player>", player.getName())
                    .replace("<uuid>", player.getUniqueId().toString());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        if (reward.isItem()) {
            giveRewardItem(player, reward.ceItemId(), reward.amount());
        }
    }

    private void giveRewardItem(Player player, String ceItemId, int amount) {
        BukkitItemDefinition definition = CraftEngineItems.byId(ceItemId);
        if (definition == null) {
            Bukkit.getLogger().warning("[Worldme-Fishing] 奖励物品 " + ceItemId + " 不存在于 CraftEngine 中。");
            return;
        }
        ItemStack reward = definition.buildBukkitItem();
        reward.setAmount(Math.max(1, amount));
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    public void saveData() {
        data.save();
    }

    public FishingConfig config() {
        return config;
    }

    public QuestKeys keys() {
        return keys;
    }

    public record QuestState(String cycle, QuestFish questFish, boolean caught, boolean completed) {
    }
}
