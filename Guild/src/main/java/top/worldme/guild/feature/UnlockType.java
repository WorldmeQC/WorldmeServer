package top.worldme.guild.feature;

import java.util.Locale;

/**
 * 功能解锁方式。
 */
public enum UnlockType {
    LEVEL,
    STRUCTURE,
    BOTH;

    public boolean needsStructure() {
        return this == STRUCTURE || this == BOTH;
    }

    public boolean needsLevel() {
        return this == LEVEL || this == BOTH;
    }

    public static UnlockType byName(String name) {
        if (name == null) {
            return LEVEL;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LEVEL;
        }
    }
}
