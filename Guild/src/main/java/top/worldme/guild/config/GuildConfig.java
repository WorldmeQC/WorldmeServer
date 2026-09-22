package top.worldme.guild.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildConfig {

    private final JavaPlugin plugin;

    private double createCost = 10000.0;
    private double defaultJoinFee = 0.0;
    private double minJoinFee = 0.0;
    private double feeGuildShare = 0.75;
    private int noLeaveDays = 7;
    private int rejoinCooldownHours = 24;
    private final List<Long> amountPresets = new ArrayList<>(List.of(1000L, 10000L, 100000L));

    private int regionChunksX = 3;
    private int regionSectionsY = 1;
    private int regionChunksZ = 3;

    private int memberCapBase = 20;
    private int memberCapPerLevel = 5;

    private int maxLevel = 10;
    private double upgradeCost = 50000.0;
    private double upgradeCostMultiplier = 1.5;
    private int expandPerLevel = 1;

    private final Map<String, String> messages = new HashMap<>();

    public GuildConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.createCost = Math.max(0, config.getDouble("guild.create-cost", 10000.0));
        this.defaultJoinFee = Math.max(0, config.getDouble("guild.default-join-fee", 0.0));
        this.minJoinFee = Math.max(0, config.getDouble("guild.min-join-fee", 0.0));
        this.feeGuildShare = Math.max(0, Math.min(1, config.getDouble("guild.fee-guild-share", 0.75)));
        this.noLeaveDays = Math.max(0, config.getInt("guild.no-leave-days", 7));
        this.rejoinCooldownHours = Math.max(0, config.getInt("guild.rejoin-cooldown-hours", 24));

        this.amountPresets.clear();
        List<Long> presets = config.getLongList("guild.amount-presets");
        if (presets != null && !presets.isEmpty()) {
            amountPresets.addAll(presets);
        } else {
            amountPresets.addAll(List.of(1000L, 10000L, 100000L));
        }

        this.regionChunksX = Math.max(1, config.getInt("guild.region.chunks-x", 3));
        this.regionSectionsY = Math.max(1, config.getInt("guild.region.sections-y", 1));
        this.regionChunksZ = Math.max(1, config.getInt("guild.region.chunks-z", 3));

        this.memberCapBase = Math.max(0, config.getInt("guild.member-cap.base", 20));
        this.memberCapPerLevel = Math.max(0, config.getInt("guild.member-cap.per-level", 5));

        this.maxLevel = Math.max(1, config.getInt("guild.level.max", 10));
        this.upgradeCost = Math.max(0, config.getDouble("guild.level.upgrade-cost", 50000.0));
        this.upgradeCostMultiplier = Math.max(1.0, config.getDouble("guild.level.upgrade-cost-multiplier", 1.5));
        this.expandPerLevel = Math.max(0, config.getInt("guild.level.expand-per-level", 1));

        this.messages.clear();
        ConfigurationSection section = config.getConfigurationSection("messages");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                messages.put(key, section.getString(key, ""));
            }
        }
    }

    /**
     * 从 currentLevel 升级到下一级所需费用。
     */
    public double upgradeCostFor(int currentLevel) {
        if (currentLevel < 1) {
            currentLevel = 1;
        }
        double cost = upgradeCost * Math.pow(upgradeCostMultiplier, currentLevel - 1);
        return Math.round(cost * 100.0) / 100.0;
    }

    public int memberCap(int level) {
        if (memberCapBase == 0) {
            return 0;
        }
        return memberCapBase + memberCapPerLevel * Math.max(0, level - 1);
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

    public double createCost() {
        return createCost;
    }

    public double defaultJoinFee() {
        return defaultJoinFee;
    }

    public double minJoinFee() {
        return minJoinFee;
    }

    public double feeGuildShare() {
        return feeGuildShare;
    }

    public int noLeaveDays() {
        return noLeaveDays;
    }

    public int rejoinCooldownHours() {
        return rejoinCooldownHours;
    }

    public List<Long> amountPresets() {
        return amountPresets;
    }

    public int regionChunksX() {
        return regionChunksX;
    }

    public int regionSectionsY() {
        return regionSectionsY;
    }

    public int regionChunksZ() {
        return regionChunksZ;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public int expandPerLevel() {
        return expandPerLevel;
    }
}
