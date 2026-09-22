package top.worldme.territory.data;

import java.util.Locale;

/**
 * 领地全局设置项。使用位掩码便于后续扩展，无需改动数据库结构。
 */
public enum SettingFlag {

    SPAWN(true),
    TNT_EXPLODE(true),
    CREEPER_EXPLODE(true),
    FIRE_SPREAD(true),
    GLOW_PLAYERS(false);

    private final boolean defaultValue;

    SettingFlag(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public static SettingFlag byName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static int defaultMask() {
        int mask = 0;
        for (SettingFlag flag : values()) {
            if (flag.defaultValue) {
                mask |= flag.bit();
            }
        }
        return mask;
    }
}
