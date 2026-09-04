package top.worldme.itemskin.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public final class ItemAppearanceUtils {

    private ItemAppearanceUtils() {}

    // region item_model

    public static boolean hasItemModel(ItemMeta meta) {
        try {
            return meta.hasItemModel();
        } catch (NoSuchMethodError e) {
            return false;
        }
    }

    public static NamespacedKey getItemModel(ItemMeta meta) {
        try {
            return meta.hasItemModel() ? meta.getItemModel() : null;
        } catch (NoSuchMethodError e) {
            return null;
        }
    }

    public static void setItemModel(ItemMeta meta, NamespacedKey model) {
        try {
            if (model == null) {
                // Paper 26.2 支持传 null 移除组件；如果后续 API 提供 resetItemModel() 可替换
                meta.setItemModel(null);
            } else {
                meta.setItemModel(model);
            }
        } catch (NoSuchMethodError ignored) {
        }
    }

    // endregion

    // region custom_model_data

    public static boolean hasCustomModelData(ItemMeta meta) {
        return meta.hasCustomModelData();
    }

    public static int getCustomModelData(ItemMeta meta) {
        return meta.hasCustomModelData() ? meta.getCustomModelData() : -1;
    }

    public static void setCustomModelData(ItemMeta meta, Integer cmd) {
        meta.setCustomModelData(cmd);
    }

    // endregion

    // region lore

    public static List<String> getLore(ItemMeta meta) {
        return meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
    }

    public static void setLore(ItemMeta meta, List<String> lore) {
        meta.setLore(lore.isEmpty() ? null : lore);
    }

    public static void appendLore(ItemMeta meta, String line) {
        List<String> lore = getLore(meta);
        lore.add(line);
        setLore(meta, lore);
    }

    public static boolean removeLoreSuffix(ItemMeta meta, String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return false;
        }
        List<String> lore = getLore(meta);
        if (lore.isEmpty()) {
            return false;
        }
        int lastIndex = lore.size() - 1;
        if (suffix.equals(lore.get(lastIndex))) {
            lore.remove(lastIndex);
            setLore(meta, lore);
            return true;
        }
        return false;
    }

    // endregion

    // region PDC helpers

    public static void setString(PersistentDataContainer pdc, NamespacedKey key, String value) {
        if (value == null) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.STRING, value);
        }
    }

    public static String getString(PersistentDataContainer pdc, NamespacedKey key) {
        return pdc.has(key, PersistentDataType.STRING) ? pdc.get(key, PersistentDataType.STRING) : null;
    }

    public static void setInt(PersistentDataContainer pdc, NamespacedKey key, int value) {
        pdc.set(key, PersistentDataType.INTEGER, value);
    }

    public static int getInt(PersistentDataContainer pdc, NamespacedKey key, int defaultValue) {
        return pdc.has(key, PersistentDataType.INTEGER) ? pdc.get(key, PersistentDataType.INTEGER) : defaultValue;
    }

    public static void remove(PersistentDataContainer pdc, NamespacedKey key) {
        pdc.remove(key);
    }

    // endregion

    public static void updateItemMeta(ItemStack item, ItemMeta meta) {
        item.setItemMeta(meta);
    }
}
