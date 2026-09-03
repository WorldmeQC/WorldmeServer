package top.worldme.trigger.config;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerConfig {

    private final JavaPlugin plugin;
    private final Map<String, WorldConfig> worlds = new HashMap<>();

    public TriggerConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        worlds.clear();

        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection == null) {
            plugin.getLogger().warning("配置文件中未找到 worlds 节点。");
            return;
        }

        for (String key : worldsSection.getKeys(false)) {
            ConfigurationSection section = worldsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String envName = section.getString("environment");
            World.Environment environment;
            try {
                environment = World.Environment.valueOf(envName);
            } catch (IllegalArgumentException | NullPointerException e) {
                plugin.getLogger().warning("世界 " + key + " 的 environment 配置无效: " + envName);
                continue;
            }

            String worldName = section.getString("world-name", key);
            List<String> permissions = section.getStringList("permissions");
            String actionbarMessage = section.getString("actionbar-message", "");

            worlds.put(key, new WorldConfig(key, environment, worldName, permissions, actionbarMessage));
        }
    }

    public Map<String, WorldConfig> getWorlds() {
        return Collections.unmodifiableMap(worlds);
    }

    public WorldConfig getWorldConfig(World.Environment environment) {
        for (WorldConfig config : worlds.values()) {
            if (config.getEnvironment() == environment) {
                return config;
            }
        }
        return null;
    }

    public static class WorldConfig {

        private final String key;
        private final World.Environment environment;
        private final String worldName;
        private final List<String> permissions;
        private final String actionbarMessage;

        public WorldConfig(String key, World.Environment environment, String worldName,
                           List<String> permissions, String actionbarMessage) {
            this.key = key;
            this.environment = environment;
            this.worldName = worldName;
            this.permissions = permissions;
            this.actionbarMessage = actionbarMessage;
        }

        public String getKey() {
            return key;
        }

        public World.Environment getEnvironment() {
            return environment;
        }

        public String getWorldName() {
            return worldName;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public String getActionbarMessage() {
            return actionbarMessage;
        }
    }
}
