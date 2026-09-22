package top.worldme.territory.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import top.worldme.Territory;
import top.worldme.territory.config.TerritoryMenuConfig;
import top.worldme.territory.config.TerritoryMenuConfig.DialogConfig;
import top.worldme.territory.config.TerritoryMenuConfig.ItemConfig;
import top.worldme.territory.data.Region;
import top.worldme.territory.manager.RegionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public abstract class TerritoryMenu implements InventoryHolder {

    protected final Territory plugin;
    protected final TerritoryMenuConfig menu;
    protected final RegionManager manager;
    protected final Player player;
    protected final Inventory inventory;

    protected TerritoryMenu(Territory plugin, Player player, String title, int rows) {
        this.plugin = plugin;
        this.menu = plugin.getMenuConfig();
        this.manager = plugin.getRegionManager();
        this.player = player;
        this.inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows)) * 9, mini(title));
    }

    public abstract void render();

    public abstract void handleClick(InventoryClickEvent event);

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

    protected ItemStack buildHead(OfflinePlayer owner, ItemConfig itemConfig, Map<String, String> placeholders) {
        ItemStack item = buildItem(itemConfig, placeholders);
        if (item == null) {
            return null;
        }
        if (item.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(owner);
            item.setItemMeta(skull);
        }
        return item;
    }

    protected Map<String, String> regionPlaceholders(Region region) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", region.name());
        placeholders.put("world", region.world());
        placeholders.put("bounds", region.boundsString());
        placeholders.put("volume", String.valueOf(region.volumeUnits()));
        placeholders.put("members", String.valueOf(region.members().size()));
        placeholders.put("warp", region.hasWarp() ? "<green>已设置</green>" : "<red>未设置</red>");
        placeholders.put("enter", region.enterMessage() == null ? "<dark_gray>默认</dark_gray>" : "<white>" + region.enterMessage() + "</white>");
        placeholders.put("leave", region.leaveMessage() == null ? "<dark_gray>默认</dark_gray>" : "<white>" + region.leaveMessage() + "</white>");
        return placeholders;
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
        String text = plugin.getTerritoryConfig().getMessage(key, placeholders);
        if (text == null || text.isEmpty()) {
            return;
        }
        player.sendMessage(mini(plugin.getTerritoryConfig().getMessage("prefix") + text));
    }

    protected void open(TerritoryMenu target) {
        player.openInventory(target.getInventory());
    }

    protected void refresh() {
        inventory.clear();
        render();
    }

    /**
     * 弹出文本输入对话框，确认后执行 setter，并在之后执行 after。
     */
    protected void showTextDialog(DialogConfig cfg, String initial, Consumer<String> setter, Runnable after) {
        DialogInput input = DialogInput.text(
                "value",
                cfg.width,
                mini(cfg.label),
                true,
                initial == null ? "" : initial,
                cfg.maxLength,
                null
        );
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mini(cfg.title))
                        .inputs(List.of(input))
                        .canCloseWithEscape(false)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(mini(cfg.confirm), null, 100,
                                DialogAction.customClick((response, audience) -> {
                                    String value = response.getText("value");
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        setter.accept(value);
                                        after.run();
                                    });
                                }, ClickCallback.Options.builder().uses(1).build())),
                        ActionButton.create(mini(cfg.cancel), null, 100,
                                DialogAction.customClick((response, audience) ->
                                                Bukkit.getScheduler().runTask(plugin, after),
                                        ClickCallback.Options.builder().uses(1).build()))
                )));
        player.showDialog(dialog);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
