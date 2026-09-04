package top.worldme.itemskin.utils;

import org.bukkit.NamespacedKey;

public final class ItemSkinKeys {

    private ItemSkinKeys() {}

    public static final String NAMESPACE = "worldme";

    public static final NamespacedKey SKIN_ID = new NamespacedKey(NAMESPACE, "itemskin_skin_id");
    public static final NamespacedKey ORIGINAL_MODEL = new NamespacedKey(NAMESPACE, "itemskin_original_model");
    public static final NamespacedKey ORIGINAL_CMD = new NamespacedKey(NAMESPACE, "itemskin_original_cmd");
    public static final NamespacedKey LORE_SUFFIX = new NamespacedKey(NAMESPACE, "itemskin_lore_suffix");
}
