package top.worldme.territory.data;

import java.util.Locale;

/**
 * 成员可操作的权限位。使用位掩码便于后续扩展，无需改动数据库结构。
 */
public enum PermissionFlag {

    BUILD,
    BREAK,
    USE_CONTAINER,
    USE_DOOR,
    USE_BUTTON,
    INTERACT_ENTITY,
    TELEPORT;

    public int bit() {
        return 1 << ordinal();
    }

    public static PermissionFlag byName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 全部权限的位掩码。
     */
    public static int all() {
        int mask = 0;
        for (PermissionFlag flag : values()) {
            mask |= flag.bit();
        }
        return mask;
    }
}
