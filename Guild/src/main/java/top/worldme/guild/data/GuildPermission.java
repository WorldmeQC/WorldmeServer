package top.worldme.guild.data;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 公会职级权限位。使用位掩码便于扩展，无需改动数据库结构。
 */
public enum GuildPermission {

    BUILD,
    BREAK,
    USE_CONTAINER,
    USE_DOOR,
    USE_BUTTON,
    INTERACT_ENTITY,
    TELEPORT,
    MANAGE_MEMBERS,
    MANAGE_RANKS,
    MANAGE_CLAIMS,
    MANAGE_FEATURES,
    MANAGE_SETTINGS,
    DEPOSIT,
    WITHDRAW,
    KICK,
    INVITE;

    public int bit() {
        return 1 << ordinal();
    }

    public static GuildPermission byName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static int mask(Collection<GuildPermission> permissions) {
        int mask = 0;
        for (GuildPermission permission : permissions) {
            mask |= permission.bit();
        }
        return mask;
    }

    public static int mask(GuildPermission... permissions) {
        return mask(List.of(permissions));
    }

    public static int all() {
        int mask = 0;
        for (GuildPermission permission : values()) {
            mask |= permission.bit();
        }
        return mask;
    }
}
