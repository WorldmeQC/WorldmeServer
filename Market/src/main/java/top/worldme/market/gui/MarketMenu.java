package top.worldme.market.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import top.worldme.Market;
import top.worldme.market.config.MarketMenuConfig;
import top.worldme.market.config.MarketMenuConfig.ItemConfig;
import top.worldme.market.manager.MarketManager;
import top.worldme.market.util.ItemParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class MarketMenu implements InventoryHolder {

    protected final Market plugin;
    protected final MarketMenuConfig menu;
    protected final MarketManager manager;
    protected final Player player;
    protected final Inventory inventory;

    protected MarketMenu(Market plugin, Player player, String title, int rows) {
        this.plugin = plugin;
        this.menu = plugin.getMenuConfig();
        this.manager = plugin.getMarketManager();
        this.player = player;
        this.inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows)) * 9, mini(title));
    }

    public abstract void render();

    public abstract void handleClick(InventoryClickEvent event);

    /**
     * 允许玩家直接操作的槽位（如鼠标拾取/放入）。默认全部取消。
     */
    public boolean isEditableSlot(int slot) {
        return false;
    }

    public boolean allowPlayerInventoryClick() {
        return false;
    }

    public void onClose() {
    }

    protected void applyBackground(int... skipSlots) {
        if (!menu.fillBackground) {
            return;
        }
        ItemStack background = buildItem(menu.background, null);
        if (background == null) {
            return;
        }
        outer:
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            for (int skip : skipSlots) {
                if (slot == skip) {
                    continue outer;
                }
            }
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType().isAir()) {
                inventory.setItem(slot, background.clone());
            }
        }
    }

    protected void place(int slot, ItemConfig itemConfig, Map<String, String> placeholders) {
        if (slot < 0 || slot >= inventory.getSize() || itemConfig == null) {
            return;
        }
        ItemStack item = buildItem(itemConfig, placeholders);
        if (item != null) {
            inventory.setItem(slot, item);
        }
    }

    protected ItemStack buildItem(ItemConfig itemConfig, Map<String, String> placeholders) {
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
            meta.displayName(mini(applyPlaceholders(itemConfig.name, placeholders)));
        }
        if (!itemConfig.lore.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : itemConfig.lore) {
                lore.add(mini(applyPlaceholders(line, placeholders)));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    protected void appendLore(ItemStack item, List<String> lines, Map<String, String> placeholders) {
        if (item == null || lines == null || lines.isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        for (String line : lines) {
            lore.add(mini(applyPlaceholders(line, placeholders)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    protected String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null) {
            return text;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    protected Component mini(String text) {
        return MiniMessage.miniMessage().deserialize(text == null ? "" : text);
    }

    protected void sendMessage(String key, Map<String, String> placeholders) {
        String text = plugin.getMarketConfig().getMessage(key, placeholders);
        if (text == null || text.isEmpty()) {
            return;
        }
        player.sendMessage(mini(plugin.getMarketConfig().getMessage("prefix") + text));
    }

    protected void open(MarketMenu target) {
        player.openInventory(target.getInventory());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
