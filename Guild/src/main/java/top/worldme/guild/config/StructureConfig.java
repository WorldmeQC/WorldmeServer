package top.worldme.guild.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.guild.feature.UnlockType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class StructureConfig {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, StructurePattern> structures = new HashMap<>();
    private final Map<String, FeatureDefinition> features = new HashMap<>();

    public StructureConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "structures.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("structures.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        structures.clear();
        features.clear();

        ConfigurationSection structuresSection = config.getConfigurationSection("structures");
        if (structuresSection != null) {
            for (String key : structuresSection.getKeys(false)) {
                ConfigurationSection section = structuresSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                ConfigurationSection bounds = section.getConfigurationSection("bounds");
                int bx = bounds == null ? 1 : Math.max(1, bounds.getInt("x", 1));
                int by = bounds == null ? 1 : Math.max(1, bounds.getInt("y", 1));
                int bz = bounds == null ? 1 : Math.max(1, bounds.getInt("z", 1));

                List<StructurePattern.BlockRequirement> requirements = new ArrayList<>();
                for (Map<?, ?> map : section.getMapList("pattern")) {
                    Object offsetObj = map.get("offset");
                    if (!(offsetObj instanceof List<?> offset) || offset.size() < 3) {
                        plugin.getLogger().warning("结构 " + key + " 的 pattern 缺少有效的 offset，已跳过该项。");
                        continue;
                    }
                    String materialName = Objects.toString(map.get("material"), null);
                    Material material = parseMaterial(materialName);
                    if (material == null) {
                        plugin.getLogger().warning("结构 " + key + " 的材质无效: " + materialName);
                        continue;
                    }
                    requirements.add(new StructurePattern.BlockRequirement(
                            toInt(offset.get(0)), toInt(offset.get(1)), toInt(offset.get(2)), material));
                }
                structures.put(key, new StructurePattern(key, bx, by, bz, requirements));
            }
        }

        ConfigurationSection featuresSection = config.getConfigurationSection("features");
        if (featuresSection != null) {
            for (String key : featuresSection.getKeys(false)) {
                ConfigurationSection section = featuresSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                String type = section.getString("type", key);
                String structure = section.getString("structure", null);
                UnlockType unlockType = UnlockType.byName(section.getString("unlocked-by", "level"));
                int requiredLevel = Math.max(1, section.getInt("required-level", 1));

                Map<String, Object> options = new HashMap<>();
                ConfigurationSection optionsSection = section.getConfigurationSection("options");
                if (optionsSection != null) {
                    for (String optionKey : optionsSection.getKeys(false)) {
                        options.put(optionKey, optionsSection.get(optionKey));
                    }
                }
                features.put(key, new FeatureDefinition(key, type, structure, unlockType, requiredLevel, options));
            }
        }
    }

    private Material parseMaterial(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int toInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public StructurePattern structure(String key) {
        return structures.get(key);
    }

    public FeatureDefinition feature(String key) {
        return features.get(key);
    }

    public Map<String, StructurePattern> structures() {
        return Collections.unmodifiableMap(structures);
    }

    public Map<String, FeatureDefinition> features() {
        return Collections.unmodifiableMap(features);
    }
}
