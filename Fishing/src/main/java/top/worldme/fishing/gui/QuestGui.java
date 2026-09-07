package top.worldme.fishing.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import top.worldme.fishing.config.FishingConfig.QuestFish;
import top.worldme.fishing.config.MenuConfig;
import top.worldme.fishing.config.MenuConfig.ItemConfig;
import top.worldme.fishing.quest.QuestManager;
import top.worldme.fishing.quest.QuestManager.QuestState;
import top.worldme.fishing.util.ItemParser;

import java.util.*;

public class QuestGui implements InventoryHolder {

    private final QuestManager questManager;
    private final MenuConfig menuConfig;
    private final Player player;
    private final Inventory inventory;

    public QuestGui(QuestManager questManager, MenuConfig menuConfig, Player player) {
        this.questManager = questManager;
        this.menuConfig = menuConfig;
        this.player = player;
        this.inventory = Bukkit.createInventory(
                this,
                menuConfig.size(),
                MiniMessage.miniMessage().deserialize(menuConfig.title())
        );
        render();
    }

    private void render() {
        inventory.clear();
        QuestState state = questManager.ensureQuest(player);
        QuestFish questFish = state.questFish();

        // 装饰
        ItemStack decoration = buildItem(menuConfig.decorationItem(), 1, null);
        for (int slot : menuConfig.decorationSlots()) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, decoration.clone());
            }
        }

        // 任务信息
        ItemStack infoItem;
        if (questFish != null) {
            infoItem = buildQuestInfoItem(questFish, state);
        } else {
            infoItem = buildItem(menuConfig.questInfoFallback(), 1, null);
        }
        int infoSlot = menuConfig.questInfoSlot();
        if (infoSlot >= 0 && infoSlot < inventory.getSize()) {
            inventory.setItem(infoSlot, infoItem);
        }

        // 完成按钮
        boolean canComplete = questManager.canComplete(player) && questManager.findValidQuestFish(player) != null;
        ItemConfig buttonConfig = canComplete ? menuConfig.completeReady() : menuConfig.completeNotReady();
        ItemStack button = buildItem(buttonConfig, 1, null);
        int completeSlot = menuConfig.completeSlot();
        if (completeSlot >= 0 && completeSlot < inventory.getSize()) {
            inventory.setItem(completeSlot, button);
        }
    }

    private ItemStack buildQuestInfoItem(QuestFish questFish, QuestState state) {
        BukkitItemDefinition definition = CraftEngineItems.byId(questFish.ceItemId());
        ItemStack base;
        if (definition != null) {
            base = definition.buildBukkitItem();
        } else {
            base = buildItem(menuConfig.questInfoFallback(), 1, null);
        }
        if (base == null || base.getType().isAir()) {
            base = new ItemStack(Material.BARRIER);
        }
        base = base.clone();
        base.setAmount(1);

        String status;
        if (state.completed()) {
            status = "已完成";
        } else if (state.caught()) {
            status = "已钓到，等待上交";
        } else {
            status = "进行中";
        }

        Map<String, String> placeholders = new HashMap<>();
        String fishName = PlainTextComponentSerializer.plainText().serialize(base.displayName());
        if (fishName.isBlank()) {
            fishName = questFish.ceItemId();
        }
        placeholders.put("fish_name", fishName);
        placeholders.put("status", status);

        // menu.yml 中 quest-info 的 lore 里可以用 %lore% 占位符，
        // 会展开为该任务鱼配置的 lore 行
        List<String> finalLore = new ArrayList<>();
        for (String line : menuConfig.questInfoLore()) {
            if (line.contains("%lore%")) {
                if (questFish.lore().isEmpty()) {
                    finalLore.add(line.replace("%lore%", ""));
                } else {
                    for (String fishLoreLine : questFish.lore()) {
                        finalLore.add(line.replace("%lore%", fishLoreLine));
                    }
                }
            } else {
                finalLore.add(line);
            }
        }

        applyItemText(base, menuConfig.questInfoName(), finalLore, placeholders);
        return base;
    }

    private void applyItemText(ItemStack item, String name, List<String> lore, Map<String, String> placeholders) {
        // 注意：不能用 item.hasItemMeta() 判断，新建的物品还没有存储 meta 会返回 false，
        // 但 getItemMeta() 始终会返回一份可编辑的 meta 副本
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (name != null && !name.isEmpty()) {
            meta.displayName(MiniMessage.miniMessage().deserialize(applyPlaceholders(name, placeholders)));
        }
        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> components = new ArrayList<>();
            for (String line : lore) {
                components.add(MiniMessage.miniMessage().deserialize(applyPlaceholders(line, placeholders)));
            }
            meta.lore(components);
        }
        item.setItemMeta(meta);
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (placeholders == null) {
            return text;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    private ItemStack buildItem(ItemConfig config, int amount, Map<String, String> placeholders) {
        ItemStack item = ItemParser.buildItem(config.material, amount);
        if (item == null) {
            item = new ItemStack(Material.STONE);
        }
        applyItemText(item, config.name, config.lore, placeholders);
        return item;
    }

    public void handleClick(int slot) {
        if (slot != menuConfig.completeSlot()) {
            return;
        }
        if (!questManager.canComplete(player)) {
            return;
        }
        if (questManager.tryComplete(player)) {
            player.closeInventory();
            sendMessage("reward-received");
        } else {
            player.closeInventory();
            sendMessage("no-valid-quest-fish");
        }
    }

    private void sendMessage(String key) {
        String text = questManager.config().getMessage(key);
        if (text == null || text.isEmpty()) {
            return;
        }
        String prefix = questManager.config().getMessage("prefix");
        player.sendMessage(MiniMessage.miniMessage().deserialize(prefix + text));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public static class GuiListener implements Listener {

        private final QuestManager questManager;
        private final MenuConfig menuConfig;

        public GuiListener(QuestManager questManager, MenuConfig menuConfig) {
            this.questManager = questManager;
            this.menuConfig = menuConfig;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof QuestGui gui)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getInventory()) {
                return;
            }
            gui.handleClick(event.getSlot());
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof QuestGui) {
                event.setCancelled(true);
            }
        }
    }
}
