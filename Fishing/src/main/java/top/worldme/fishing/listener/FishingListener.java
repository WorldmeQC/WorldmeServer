package top.worldme.fishing.listener;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.event.CustomFishingReloadEvent;
import net.momirealms.customfishing.api.event.FishingLootSpawnEvent;
import net.momirealms.customfishing.api.event.FishingResultEvent;
import net.momirealms.customfishing.api.event.MarketSellEvent;
import net.momirealms.customfishing.api.mechanic.action.ActionManager;
import net.momirealms.customfishing.api.mechanic.context.ContextKeys;
import net.momirealms.customfishing.api.mechanic.requirement.RequirementFactory;
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
import top.worldme.fishing.util.QuestKeys;

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

    public void registerCustomRequirement() {
        if (Bukkit.getPluginManager().getPlugin("CustomFishing") == null) {
            plugin.getLogger().warning("未检测到 CustomFishing，任务鱼可钓控制将不会生效。");
            return;
        }
        try {
            BukkitCustomFishingPlugin api = BukkitCustomFishingPlugin.getInstance();
            RequirementFactory<Player> factory = (args, actions, runActions) -> context -> {
                Player player = context.holder();
                if (player == null) {
                    return false;
                }
                String lootId = context.arg(ContextKeys.ID);
                if (lootId == null) {
                    return false;
                }
                if (questManager.isQuestActiveForLoot(player, lootId)) {
                    return true;
                }
                if (runActions && !actions.isEmpty()) {
                    ActionManager.trigger(context, actions);
                }
                return false;
            };
            boolean registered = api.getRequirementManager().registerRequirement(factory, "fisherman_quest");
            if (registered) {
                plugin.getLogger().info("已向 CustomFishing 注册 fisherman_quest 条件。");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("注册 CustomFishing 自定义条件失败: " + e.getMessage());
            e.printStackTrace();
        }
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
        if (!questManager.isQuestActiveForLoot(player, lootId)) {
            event.setCancelled(true);
            return;
        }

        questManager.markCaught(player);
        // 同时给背包里已获得的该任务鱼打上 PDC（兼容直接进背包/钓鱼袋的情况）
        questManager.tagInventoryQuestFish(player, questFish);
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onCustomFishingReload(CustomFishingReloadEvent event) {
        // CustomFishing 重载后重新注册条件
        Bukkit.getScheduler().runTask(plugin, this::registerCustomRequirement);
    }
}
