package top.worldme.cleaner.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.cleaner.config.CleanerConfig;
import top.worldme.cleaner.config.TrashMenuConfig;
import top.worldme.cleaner.config.TrashMenuConfig.ItemConfig;
import top.worldme.cleaner.manager.ClearManager;
import top.worldme.cleaner.manager.ClearManager.TrashEntry;
import top.worldme.cleaner.util.ItemParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrashCanGui implements InventoryHolder {

    private final JavaPlugin plugin;
    private final TrashMenuConfig menu;
    private final CleanerConfig config;
    private final ClearManager clearManager;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    public TrashCanGui(JavaPlugin plugin, TrashMenuConfig menu, CleanerConfig config,
                       ClearManager clearManager, Player player) {
        this.plugin = plugin;
        this.menu = menu;
        this.config = config;
        this.clearManager = clearManager;
        this.player = player;
        this.inventory = Bukkit.createInventory(
                this,
                menu.size(),
                MiniMessage.miniMessage().deserialize(menu.title())
        );
        render();
    }

    private int maxPage() {
        int slots = menu.contentSlots().size();
        if (slots <= 0) {
            return 0;
        }
        return Math.max(0, (clearManager.trashSize() - 1) / slots);
    }

    private void render() {
        inventory.clear();
        page = Math.max(0, Math.min(page, maxPage()));

        ItemStack decoration = buildItem(menu.decoration(), null);
        if (decoration != null) {
            for (int slot : menu.decorationSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, decoration.clone());
                }
            }
        }

        ItemStack info = buildItem(menu.info(), Map.of(
                "count", String.valueOf(clearManager.trashSize()),
                "page", String.valueOf(page + 1),
                "pages", String.valueOf(maxPage() + 1)
        ));
        if (info != null && menu.infoSlot() >= 0 && menu.infoSlot() < inventory.getSize()) {
            inventory.setItem(menu.infoSlot(), info);
        }

        ItemStack prev = buildItem(page > 0 ? menu.prevButton() : menu.prevButtonDisabled(), null);
        if (prev != null && menu.prevSlot() >= 0 && menu.prevSlot() < inventory.getSize()) {
            inventory.setItem(menu.prevSlot(), prev);
        }
        ItemStack next = buildItem(page < maxPage() ? menu.nextButton() : menu.nextButtonDisabled(), null);
        if (next != null && menu.nextSlot() >= 0 && menu.nextSlot() < inventory.getSize()) {
            inventory.setItem(menu.nextSlot(), next);
        }

        List<TrashEntry> trash = clearManager.getTrash();
        List<Integer> slots = menu.contentSlots();
        int start = page * slots.size();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            int index = start + i;
            if (index < trash.size()) {
                inventory.setItem(slot, trash.get(index).item().clone());
            }
        }
    }

    public void handleClick(int slot) {
        if (slot == menu.prevSlot()) {
            if (page > 0) {
                page--;
                render();
            } else {
                sendMessage("first-page");
            }
            return;
        }
        if (slot == menu.nextSlot()) {
            if (page < maxPage()) {
                page++;
                render();
            } else {
                sendMessage("last-page");
            }
            return;
        }
        if (!menu.contentSlots().contains(slot)) {
            return;
        }
        ItemStack current = inventory.getItem(slot);
        if (current == null || current.getType().isAir()) {
            return;
        }
        if (clearManager.removeFromTrash(current)) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(current);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            render();
        }
    }

    private void sendMessage(String key) {
        String text = config.getMessage(key);
        if (text == null || text.isEmpty()) {
            return;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
    }

    private ItemStack buildItem(ItemConfig itemConfig, Map<String, String> placeholders) {
        if (itemConfig == null || itemConfig.material == null || itemConfig.material.isBlank()) {
            return null;
        }
        ItemStack item = ItemParser.buildItem(itemConfig.material, 1);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (itemConfig.name != null && !itemConfig.name.isEmpty()) {
            meta.displayName(MiniMessage.miniMessage().deserialize(applyPlaceholders(itemConfig.name, placeholders)));
        }
        if (!itemConfig.lore.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : itemConfig.lore) {
                lore.add(MiniMessage.miniMessage().deserialize(applyPlaceholders(line, placeholders)));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
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

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public static class GuiListener implements Listener {

        private final JavaPlugin plugin;
        private final TrashMenuConfig menu;
        private final ClearManager clearManager;

        public GuiListener(JavaPlugin plugin, TrashMenuConfig menu, ClearManager clearManager) {
            this.plugin = plugin;
            this.menu = menu;
            this.clearManager = clearManager;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof TrashCanGui gui)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getInventory()) {
                return;
            }
            gui.handleClick(event.getSlot());
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof TrashCanGui) {
                event.setCancelled(true);
            }
        }
    }
}