package top.worldme.cleaner.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CleanerConfig {

    private static final List<Integer> DEFAULT_REMINDERS = List.of(180, 120, 60, 30, 20, 10, 5, 4, 3, 2, 1);

    private final JavaPlugin plugin;

    private boolean enabled = true;
    private int interval = 300;
    private boolean trashEnabled = true;
    private int trashMaxItems = 54;
    private final Set<String> worlds = new HashSet<>();
    private final List<Integer> reminders = new ArrayList<>(DEFAULT_REMINDERS);
    private final Map<String, String> messages = new HashMap<>();

    public CleanerConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("enabled", true);
        this.interval = Math.max(1, config.getInt("interval", 300));
        this.trashEnabled = config.getBoolean("trash-enabled", true);
        this.trashMaxItems = Math.max(0, config.getInt("trash-max-items", 54));

        this.worlds.clear();
        for (String world : config.getStringList("worlds")) {
            if (world != null && !world.isBlank()) {
                worlds.add(world.trim());
            }
        }

        this.reminders.clear();
        List<Integer> configured = config.getIntegerList("reminders");
        if (configured.isEmpty()) {
            this.reminders.addAll(DEFAULT_REMINDERS);
        } else {
            for (int reminder : configured) {
                if (reminder > 0) {
                    reminders.add(reminder);
                }
            }
            if (this.reminders.isEmpty()) {
                this.reminders.addAll(DEFAULT_REMINDERS);
            }
        }

        this.messages.clear();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key, ""));
            }
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public int interval() {
        return interval;
    }

    public boolean trashEnabled() {
        return trashEnabled;
    }

    public int trashMaxItems() {
        return trashMaxItems;
    }

    public boolean shouldClearWorld(String worldName) {
        return worlds.isEmpty() || worlds.contains(worldName);
    }

    public List<Integer> reminders() {
        return Collections.unmodifiableList(reminders);
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String text = messages.getOrDefault(key, "");
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return text;
    }
}