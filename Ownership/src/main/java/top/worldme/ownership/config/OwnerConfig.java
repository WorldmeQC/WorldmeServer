package top.worldme.ownership.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class OwnerConfig {

    private final JavaPlugin plugin;

    private double price = 1000.0;
    private double unbindPrice = 0.0;
    private boolean bindWholeStack = true;
    private boolean keepOnDeath = true;
    private boolean notifyOnPickupDeny = true;
    private final Set<String> blacklist = new HashSet<>();
    private final Map<String, Double> priceOverrides = new HashMap<>();
    private boolean loreEnabled = true;
    private final List<String> loreLines = new ArrayList<>();
    private final Map<String, String> messages = new HashMap<>();

    public OwnerConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.price = config.getDouble("price", 1000.0);
        this.unbindPrice = config.getDouble("unbind-price", 0.0);
        this.bindWholeStack = config.getBoolean("bind-whole-stack", true);
        this.keepOnDeath = config.getBoolean("keep-on-death", true);
        this.notifyOnPickupDeny = config.getBoolean("notify-on-pickup-deny", true);

        this.blacklist.clear();
        for (String entry : config.getStringList("blacklist")) {
            if (entry != null && !entry.isBlank()) {
                blacklist.add(entry.trim().toUpperCase(Locale.ROOT));
            }
        }

        this.priceOverrides.clear();
        ConfigurationSection overrides = config.getConfigurationSection("price-overrides");
        if (overrides != null) {
            for (String key : overrides.getKeys(false)) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                priceOverrides.put(key.trim().toUpperCase(Locale.ROOT), overrides.getDouble(key));
            }
        }

        this.loreEnabled = config.getBoolean("lore.enabled", true);
        this.loreLines.clear();
        this.loreLines.addAll(config.getStringList("lore.lines"));

        this.messages.clear();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key, ""));
            }
        }
    }

    public double getPrice(String itemId) {
        if (itemId != null) {
            Double override = priceOverrides.get(itemId.toUpperCase(Locale.ROOT));
            if (override != null) {
                return override;
            }
        }
        return price;
    }

    public boolean isBlacklisted(String itemId) {
        return itemId != null && blacklist.contains(itemId.toUpperCase(Locale.ROOT));
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

    public double price() {
        return price;
    }

    public double unbindPrice() {
        return unbindPrice;
    }

    public boolean bindWholeStack() {
        return bindWholeStack;
    }

    public boolean keepOnDeath() {
        return keepOnDeath;
    }

    public boolean notifyOnPickupDeny() {
        return notifyOnPickupDeny;
    }

    public boolean loreEnabled() {
        return loreEnabled;
    }

    public List<String> loreLines() {
        return Collections.unmodifiableList(loreLines);
    }
}
