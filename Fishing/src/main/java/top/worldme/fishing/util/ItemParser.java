package top.worldme.fishing.util;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemParser {

    private ItemParser() {
    }

    /**
     * 根据字符串构建物品。
     * 如果以 "ce:" 开头，则剩余部分作为 CraftEngine 物品 ID；否则按原版 Material 解析。
     */
    public static ItemStack buildItem(String source, int amount) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String trimmed = source.trim();
        ItemStack item;
        if (trimmed.toLowerCase().startsWith("ce:")) {
            String ceId = trimmed.substring(3).trim();
            BukkitItemDefinition definition = CraftEngineItems.byId(ceId);
            if (definition == null) {
                return null;
            }
            item = definition.buildBukkitItem();
        } else {
            Material material;
            try {
                material = Material.valueOf(trimmed.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
            item = new ItemStack(material);
        }
        if (item != null) {
            item.setAmount(Math.max(1, amount));
        }
        return item;
    }
}
