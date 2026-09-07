package top.worldme.fishing.listener;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.event.CustomFishingReloadEvent;
import net.momirealms.customfishing.api.event.FishingBagPreCollectEvent;
import net.momirealms.customfishing.api.event.FishingEffectApplyEvent;
import net.momirealms.customfishing.api.event.FishingLootSpawnEvent;
import net.momirealms.customfishing.api.event.FishingResultEvent;
import net.momirealms.customfishing.api.event.MarketSellEvent;
import net.momirealms.customfishing.api.mechanic.action.ActionManager;
import net.momirealms.customfishing.api.mechanic.context.ContextKeys;
import net.momirealms.customfishing.api.mechanic.effect.Effect;
import net.momirealms.customfishing.api.mechanic.loot.operation.WeightOperation;
import net.momirealms.customfishing.api.mechanic.requirement.RequirementFactory;
import net.momirealms.customfishing.common.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.fishing.config.FishingConfig.QuestFish;
import top.worldme.fishing.quest.QuestManager;
import top.worldme.fishing.quest.QuestManager.QuestState;
import top.worldme.fishing.util.QuestKeys;

import java.util.ArrayList;
import java.util.List;

public class FishingListener implements Listener {

    private final JavaPlugin plugin;
    private final QuestManager questManager;
    private final QuestKeys keys;

    public FishingListener(JavaPlugin plugin, QuestManager questManager, QuestKeys keys) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFishingResult(FishingResultEvent event) {
        if (event.getResult() != FishingResultEvent.Result.SUCCESS) {
            return;
        }
        Player player = event.getPlayer();
        String lootId = event.getLoot() == null ? null : event.getLoot().id();
        if (lootId == null) {
            return;
        }

        QuestFish questFish = questManager.config().getQuestFishByLootId(lootId);
        if (questFish == null) {
            return;
        }

        // 兜底：如果 somehow 任务鱼在不满足条件时被钓到，直接取消
        // 注：onEffectApply 已在 LOOT 阶段通过 weight operation 将不可钓的任务鱼权重置 0，
        // 此处兜底极少触发。
        if (!questManager.isQuestActiveForLoot(player, lootId)) {
            event.setCancelled(true);
            return;
        }

        questManager.markCaught(player);
        // 立即尝试给背包里已获得的该任务鱼打上 PDC
        questManager.tagInventoryQuestFish(player, questFish);
        // 延迟再次尝试，兼容 CustomFishing 直接进背包/钓鱼袋等异步交付场景
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            questManager.tagInventoryQuestFish(player, questFish);
        }, 2L);
    }

    /**
     * 在 CustomFishing 计算本次战利品前，把当前不应被钓到的任务鱼权重置 0，
     * 使其不会出现在结果中，而是从其它战利品里重新抽取。
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEffectApply(FishingEffectApplyEvent event) {
        if (event.getStage() != FishingEffectApplyEvent.Stage.LOOT) {
            return;
        }
        Effect effect = event.getEffect();
        List<Pair<String, WeightOperation>> ops = new ArrayList<>(effect.weightOperations());
        for (QuestFish questFish : questManager.config().getQuestFishes()) {
            String lootId = questFish.cfLootId();
            ops.add(Pair.of(lootId, (context, weight, map) -> {
                Player player = context.holder();
                if (player == null) {
                    return weight;
                }
                return questManager.isQuestActiveForLoot(player, lootId) ? weight : 0.0;
            }));
        }
        effect.weightOperations(ops);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLootSpawn(FishingLootSpawnEvent event) {
        Player player = event.getPlayer();
        String lootId = event.getLoot() == null ? null : event.getLoot().id();
        if (lootId == null) {
            return;
        }
        QuestFish questFish = questManager.config().getQuestFishByLootId(lootId);
        if (questFish == null) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Item item)) {
            return;
        }

        ItemStack itemStack = item.getItemStack();
        if (itemStack == null || itemStack.getType().isAir()) {
            return;
        }
        if (!itemStack.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(keys.owner, PersistentDataType.STRING) || pdc.has(keys.cycle, PersistentDataType.STRING)) {
            return;
        }

        pdc.set(keys.owner, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(keys.cycle, PersistentDataType.STRING, questManager.getCurrentCycleKey());
        itemStack.setItemMeta(meta);
    }

    /**
     * 任务鱼被收集进 CustomFishing 钓鱼袋时打上 PDC，防止提交时找不到。
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBagPreCollect(FishingBagPreCollectEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemStack();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }
        QuestState state = questManager.getState(player);
        if (state.questFish() == null || !state.caught() || state.completed()) {
            return;
        }
        if (!questManager.matchesQuestItem(item, state.questFish().ceItemId())) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(keys.owner, PersistentDataType.STRING) || pdc.has(keys.cycle, PersistentDataType.STRING)) {
            return;
        }
        pdc.set(keys.owner, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(keys.cycle, PersistentDataType.STRING, questManager.getCurrentCycleKey());
        item.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMarketSell(MarketSellEvent event) {
        List<ItemStack> items = event.getItems();
        for (ItemStack item : items) {
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(keys.owner, PersistentDataType.STRING) && pdc.has(keys.cycle, PersistentDataType.STRING)) {
                event.setCancelled(true);
                return;
            }
        }
    }

}
