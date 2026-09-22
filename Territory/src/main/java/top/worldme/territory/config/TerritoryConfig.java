package top.worldme.territory.config;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.SettingFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TerritoryConfig {

    private final JavaPlugin plugin;

    private double baseCost = 1000.0;
    private double perUnitCost = 250.0;
    private int defaultMaxClaims = 1;
    private final List<ClaimLimit> claimLimits = new ArrayList<>();
    private int maxHorizontalChunks = 16;
    private int maxVerticalSections = 0;
    private int maxMembers = 10;
    private final Set<String> worldWhitelist = new HashSet<>();
    private final Set<String> worldBlacklist = new HashSet<>();
    private final Set<PermissionFlag> defaultMemberPermissions = new HashSet<>();
    private final Map<SettingFlag, Boolean> defaultSettings = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();

    public TerritoryConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.baseCost = Math.max(0, config.getDouble("claim.base-cost", 1000.0));
        this.perUnitCost = Math.max(0, config.getDouble("claim.per-unit-cost", 250.0));
        this.defaultMaxClaims = Math.max(0, config.getInt("max-claims.default", 1));

        this.claimLimits.clear();
        ConfigurationSection limits = config.getConfigurationSection("max-claims.permissions");
        if (limits != null) {
            for (String permission : limits.getKeys(false)) {
                if (permission == null || permission.isBlank()) {
                    continue;
                }
                claimLimits.add(new ClaimLimit(permission.trim(), Math.max(0, limits.getInt(permission))));
            }
        }

        this.maxHorizontalChunks = Math.max(1, config.getInt("max-size.horizontal-chunks", 16));
        this.maxVerticalSections = Math.max(0, config.getInt("max-size.vertical-sections", 0));
        this.maxMembers = Math.max(0, config.getInt("members.max", 10));

        this.worldWhitelist.clear();
        for (String entry : config.getStringList("worlds.whitelist")) {
            if (entry != null && !entry.isBlank()) {
                worldWhitelist.add(entry.trim());
            }
        }
        this.worldBlacklist.clear();
        for (String entry : config.getStringList("worlds.blacklist")) {
            if (entry != null && !entry.isBlank()) {
                worldBlacklist.add(entry.trim());
            }
        }

        this.defaultMemberPermissions.clear();
        for (String raw : config.getStringList("defaults.member-permissions")) {
            PermissionFlag flag = PermissionFlag.byName(raw);
            if (flag != null) {
                defaultMemberPermissions.add(flag);
            }
        }

        this.defaultSettings.clear();
        ConfigurationSection settingsSection = config.getConfigurationSection("defaults.settings");
        if (settingsSection != null) {
            for (String key : settingsSection.getKeys(false)) {
                SettingFlag flag = SettingFlag.byName(key);
                if (flag != null) {
                    defaultSettings.put(flag, settingsSection.getBoolean(key, flag.defaultValue()));
                }
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

    /**
     * 指定单位数对应的费用（含基础费用）。
     */
    public double costForUnits(int units) {
        return round(baseCost + perUnitCost * Math.max(0, units));
    }

    /**
     * 指定玩家可拥有的领地数量：取默认值与其拥有的权限节点上限中的最大值。
     */
    public int maxClaims(Player player) {
        int best = defaultMaxClaims;
        for (ClaimLimit limit : claimLimits) {
            if (limit.limit() > best && player.hasPermission(limit.permission())) {
                best = limit.limit();
            }
        }
        return best;
    }

    public boolean isWorldAllowed(World world) {
        if (world == null) {
            return false;
        }
        String name = world.getName();
        if (worldBlacklist.contains(name)) {
            return false;
        }
        return worldWhitelist.isEmpty() || worldWhitelist.contains(name);
    }

    public int defaultMemberMask() {
        int mask = 0;
        for (PermissionFlag flag : defaultMemberPermissions) {
            mask |= flag.bit();
        }
        return mask;
    }

    public int defaultSettingMask() {
        int mask = 0;
        for (SettingFlag flag : SettingFlag.values()) {
            Boolean value = defaultSettings.get(flag);
            boolean enabled = value == null ? flag.defaultValue() : value;
            if (enabled) {
                mask |= flag.bit();
            }
        }
        return mask;
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

    public double baseCost() {
        return baseCost;
    }

    public double perUnitCost() {
        return perUnitCost;
    }

    public int defaultMaxClaims() {
        return defaultMaxClaims;
    }

    public int maxHorizontalChunks() {
        return maxHorizontalChunks;
    }

    public int maxVerticalSections() {
        return maxVerticalSections;
    }

    public int maxMembers() {
        return maxMembers;
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ClaimLimit(String permission, int limit) {
    }
}
