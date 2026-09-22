package top.worldme.guild.config;

import top.worldme.guild.feature.UnlockType;

import java.util.Map;

/**
 * 公会功能定义（来自 structures.yml）。
 */
public class FeatureDefinition {

    private final String key;
    private final String type;
    private final String structureKey;
    private final UnlockType unlockType;
    private final int requiredLevel;
    private final Map<String, Object> options;

    public FeatureDefinition(String key, String type, String structureKey, UnlockType unlockType,
                             int requiredLevel, Map<String, Object> options) {
        this.key = key;
        this.type = type;
        this.structureKey = structureKey;
        this.unlockType = unlockType;
        this.requiredLevel = requiredLevel;
        this.options = options;
    }

    public String key() {
        return key;
    }

    public String type() {
        return type;
    }

    public String structureKey() {
        return structureKey;
    }

    public UnlockType unlockType() {
        return unlockType;
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    public Map<String, Object> options() {
        return options;
    }

    public Object option(String name, Object fallback) {
        return options.getOrDefault(name, fallback);
    }
}
