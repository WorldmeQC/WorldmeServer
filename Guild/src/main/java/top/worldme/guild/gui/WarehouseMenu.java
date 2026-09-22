package top.worldme.guild.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.worldme.Guild;
import top.worldme.guild.data.GuildFeature;

import java.util.Base64;
import java.util.Map;

/**
 * 公会共享仓库界面。内容持久化在 {@link GuildFeature#data()}。
 */
public class WarehouseMenu extends GuildMenuBase {

    private final top.worldme.guild.data.Guild guild;
    private final GuildFeature feature;

    public WarehouseMenu(Guild plugin, Player player, top.worldme.guild.data.Guild guild, GuildFeature feature) {
        super(plugin, player, plugin.getMenuConfig().warehouseTitle, plugin.getMenuConfig().warehouseRows);
        this.guild = guild;
        this.feature = feature;
        render();
    }

    @Override
    public void render() {
        decode(feature.data());
        place(menu.warehouseCloseSlot, menu.warehouseClose, Map.of());
    }

    @Override
    public boolean isEditableSlot(int slot) {
        return menu.warehouseContentSlots.contains(slot);
    }

    @Override
    public boolean allowPlayerInventoryClick() {
        return true;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() == menu.warehouseCloseSlot) {
            player.closeInventory();
        }
    }

    @Override
    public void onClose() {
        feature.setData(encode());
        manager.persistFeature(guild.id(), feature);
    }

    public void openFor() {
        open(this);
    }

    private String encode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < menu.warehouseContentSlots.size(); i++) {
            if (i > 0) {
                builder.append(';');
            }
            ItemStack item = inventory.getItem(menu.warehouseContentSlots.get(i));
            if (item != null && !item.getType().isAir()) {
                builder.append(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
        }
        return builder.toString();
    }

    private void decode(String data) {
        if (data == null || data.isBlank()) {
            return;
        }
        String[] parts = data.split(";", -1);
        for (int i = 0; i < parts.length && i < menu.warehouseContentSlots.size(); i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            try {
                inventory.setItem(menu.warehouseContentSlots.get(i),
                        ItemStack.deserializeBytes(Base64.getDecoder().decode(parts[i])));
            } catch (Exception ignored) {
                // 忽略损坏的存档项
            }
        }
    }
}
