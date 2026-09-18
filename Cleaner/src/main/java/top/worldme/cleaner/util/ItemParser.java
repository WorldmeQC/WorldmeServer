package top.worldme.cleaner.util;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class ItemParser {

    private ItemParser() {
    }

    /**
     * 根据字符串构建物品。
     * 如果以 "ce:" 开头，则剩余部分作为 CraftEngine 物品 ID；否则按原版 Material 解析。
     * 未安装 CraftEngine 或解析失败时返回 null。
     */
    public static ItemStack buildItem(String source, int amount) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String trimmed = source.trim();
        ItemStack item;
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("ce:")) {
            String ceId = trimmed.substring(3).trim();
            item = buildCraftEngineItem(ceId);
        } else {
            Material material;
            try {
                material = Material.valueOf(trimmed.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
            item = new ItemStack(material);
        }
        if (item == null || item.getType().isAir()) {
            return null;
        }
        item.setAmount(Math.max(1, amount));
        return item;
    }

    private static ItemStack buildCraftEngineItem(String ceId) {
        if (Bukkit.getPluginManager().getPlugin("CraftEngine") == null) {
            return null;
        }
        try {
            BukkitItemDefinition definition = CraftEngineItems.byId(ceId);
            if (definition == null) {
                return null;
            }
            return definition.buildBukkitItem();
        } catch (Throwable t) {
            return null;
        }
    }
}