package top.worldme.itemskin.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ItemType {
    SWORD,
    BOW,
    CROSSBOW,
    AXE,
    PICKAXE,
    SHOVEL,
    HOE,
    MACE,
    SPEAR,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    ELYTRA;

    /**
     * 判断某个 Material 是否属于此类型
     */
    public boolean matches(Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }
        String name = material.name();
        return switch (this) {
            case SWORD -> name.endsWith("_SWORD");
            case BOW -> material == Material.BOW;
            case CROSSBOW -> material == Material.CROSSBOW;
            case AXE -> name.endsWith("_AXE");
            case PICKAXE -> name.endsWith("_PICKAXE");
            case SHOVEL -> name.endsWith("_SHOVEL");
            case HOE -> name.endsWith("_HOE");
            case MACE -> material == Material.MACE;
            case SPEAR -> name.endsWith("_SPEAR");
            case HELMET -> name.endsWith("_HELMET");
            case CHESTPLATE -> name.endsWith("_CHESTPLATE");
            case LEGGINGS -> name.endsWith("_LEGGINGS");
            case BOOTS -> name.endsWith("_BOOTS");
            case ELYTRA -> material == Material.ELYTRA;
        };
    }

    /**
     * 根据 ItemStack 判断其属于哪种武器/装备类型
     */
    public static ItemType fromItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        Material material = item.getType();
        for (ItemType type : values()) {
            if (type.matches(material)) {
                return type;
            }
        }
        return null;
    }
}
