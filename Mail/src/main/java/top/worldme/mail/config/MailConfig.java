package top.worldme.mail.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class MailConfig {

    private final JavaPlugin plugin;

    private int expireDays = 30;
    private String defaultSubject = "系统邮件";
    private int cleanupInterval = 600;
    private String dateFormat = "yyyy-MM-dd HH:mm";
    private ZoneId zoneId = ZoneId.of("Asia/Shanghai");
    private boolean unreadReminder = true;
    private final Map<String, String> messages = new HashMap<>();

    public MailConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.expireDays = Math.max(1, config.getInt("expire-days", 30));
        this.defaultSubject = config.getString("default-subject", "系统邮件");
        this.cleanupInterval = Math.max(10, config.getInt("cleanup-interval", 600));
        this.dateFormat = config.getString("date-format", "yyyy-MM-dd HH:mm");
        try {
            this.zoneId = ZoneId.of(config.getString("time-zone", "Asia/Shanghai"));
        } catch (Exception e) {
            this.zoneId = ZoneId.of("Asia/Shanghai");
        }
        this.unreadReminder = config.getBoolean("unread-reminder", true);

        this.messages.clear();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key, ""));
            }
        }
    }

    public int expireDays() {
        return expireDays;
    }

    public String defaultSubject() {
        return defaultSubject;
    }

    public int cleanupInterval() {
        return cleanupInterval;
    }

    public String dateFormat() {
        return dateFormat;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public boolean unreadReminder() {
        return unreadReminder;
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