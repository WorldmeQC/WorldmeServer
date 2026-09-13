package top.worldme.ownership.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.ownership.binding.BindManager;
import top.worldme.ownership.config.BindMenuConfig;
import top.worldme.ownership.config.BindMenuConfig.ItemConfig;
import top.worldme.ownership.config.OwnerConfig;
import top.worldme.ownership.economy.VaultHook;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BindGui implements InventoryHolder {

    private final JavaPlugin plugin;
    private final BindMenuConfig menu;
    private final OwnerConfig config;
    private final BindManager bindManager;
    private final VaultHook vaultHook;
    private final Player player;
    private final Inventory inventory;

    public BindGui(JavaPlugin plugin, BindMenuConfig menu, OwnerConfig config, BindManager bindManager, VaultHook vaultHook, Player player) {
        this.plugin = plugin;
        this.menu = menu;
        this.config = config;
        this.bindManager = bindManager;
        this.vaultHook = vaultHook;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, menu.size(), MiniMessage.miniMessage().deserialize(menu.title()));
        render();
    }

    private void render() {
        ItemStack decoration = buildItem(menu.decoration(), null);
        if (decoration != null) {
            for (int slot : menu.decorationSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, decoration.clone());
                }
            }
        }
        ItemStack info = buildItem(menu.info(), null);
        if (info != null) {
            int slot = menu.infoSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, info);
            }
        }
        updateConfirm();
    }

    public void updateConfirm() {
        ItemStack input = inventory.getItem(menu.inputSlot());
        String itemId = (input == null || input.getType().isAir()) ? null : bindManager.itemId(input);
        double price = config.getPrice(itemId);
        ItemStack button = buildItem(menu.confirm(), Map.of("price", vaultHook.format(price)));
        if (button != null) {
            int slot = menu.confirmSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, button);
            }
        }
    }

    private ItemStack buildItem(ItemConfig itemConfig, Map<String, String> placeholders) {
        if (itemConfig == null || itemConfig.material == null || itemConfig.material.isBlank()) {
            return null;
        }
        Material material;
        try {
            material = Material.valueOf(itemConfig.material.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        ItemStack item = new ItemStack(material);
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

    public void handleConfirm() {
        ItemStack input = inventory.getItem(menu.inputSlot());
        if (input == null || input.getType().isAir()) {
            sendMessage("cannot-bind-air", null);
            return;
        }
        if (bindManager.isBound(input)) {
            sendMessage("already-owned", null);
            return;
        }
        String itemId = bindManager.itemId(input);
        if (config.isBlacklisted(itemId)) {
            sendMessage("blacklisted", null);
            return;
        }
        if (!vaultHook.isReady()) {
            sendMessage("no-economy", null);
            return;
        }
        double price = config.getPrice(itemId);
        String priceText = vaultHook.format(price);
        var economy = vaultHook.economy();
        if (price > 0 && !economy.has(player, price)) {
            sendMessage("not-enough-money", Map.of("price", priceText));
            return;
        }
        if (price > 0) {
            var response = economy.withdrawPlayer(player, price);
            if (!response.transactionSuccess()) {
                sendMessage("no-economy", null);
                return;
            }
        }

        ItemStack item = input.clone();
        if (config.bindWholeStack()) {
            inventory.setItem(menu.inputSlot(), null);
        } else {
            item.setAmount(1);
            int remaining = input.getAmount() - 1;
            if (remaining > 0) {
                ItemStack rest = input.clone();
                rest.setAmount(remaining);
                inventory.setItem(menu.inputSlot(), rest);
            } else {
                inventory.setItem(menu.inputSlot(), null);
            }
        }

        bindManager.bind(item, player);
        giveBack(item);
        updateConfirm();
        sendMessage("success", Map.of("price", priceText));
    }

    private void giveBack(ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            ItemStack current = inventory.getItem(menu.inputSlot());
            if (current == null || current.getType().isAir()) {
                inventory.setItem(menu.inputSlot(), drop);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    public void returnInput() {
        ItemStack input = inventory.getItem(menu.inputSlot());
        if (input == null || input.getType().isAir()) {
            return;
        }
        inventory.setItem(menu.inputSlot(), null);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(input);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    public void moveToInput(ItemStack clicked, InventoryClickEvent event) {
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        ItemStack current = inventory.getItem(menu.inputSlot());
        if (current != null && !current.getType().isAir()) {
            return;
        }
        inventory.setItem(menu.inputSlot(), clicked.clone());
        event.setCurrentItem(null);
        updateConfirm();
    }

    private void sendMessage(String key, Map<String, String> placeholders) {
        String text = config.getMessage(key, placeholders);
        if (text == null || text.isEmpty()) {
            return;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
    }

    public void scheduleConfirmUpdate() {
        Bukkit.getScheduler().runTask(plugin, this::updateConfirm);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public int inputSlot() {
        return menu.inputSlot();
    }

    public int confirmSlot() {
        return menu.confirmSlot();
    }

    public static class GuiListener implements Listener {

        private final BindMenuConfig menu;
        private final OwnerConfig config;
        private final BindManager bindManager;
        private final VaultHook vaultHook;

        public GuiListener(BindMenuConfig menu, OwnerConfig config, BindManager bindManager, VaultHook vaultHook) {
            this.menu = menu;
            this.config = config;
            this.bindManager = bindManager;
            this.vaultHook = vaultHook;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof BindGui gui)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClick() == ClickType.DOUBLE_CLICK) {
                return;
            }
            Inventory clicked = event.getClickedInventory();
            if (clicked == null) {
                return;
            }
            if (clicked.equals(event.getInventory())) {
                int slot = event.getSlot();
                if (slot == gui.inputSlot()) {
                    event.setCancelled(false);
                    gui.scheduleConfirmUpdate();
                } else if (slot == gui.confirmSlot()) {
                    gui.handleConfirm();
                }
            } else {
                if (event.isShiftClick()) {
                    gui.moveToInput(event.getCurrentItem(), event);
                } else {
                    event.setCancelled(false);
                }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (!(event.getInventory().getHolder() instanceof BindGui gui)) {
                return;
            }
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot == gui.inputSlot()) {
                    continue;
                }
                event.setCancelled(true);
                return;
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (!(event.getInventory().getHolder() instanceof BindGui gui)) {
                return;
            }
            gui.returnInput();
        }
    }
}
