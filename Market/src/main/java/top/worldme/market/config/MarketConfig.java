package top.worldme.market.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MarketConfig {

    private final JavaPlugin plugin;

    private String dateFormat = "yyyy-MM-dd HH:mm";
    private ZoneId zoneId = ZoneId.of("Asia/Shanghai");
    private int expireDays = 3;
    private int checkInterval = 60;
    private String listingFee = "5%";
    private double salesTax = 0.0;
    private int maxListingsPerPlayer = 10;
    private double minPrice = 1.0;
    private double maxPrice = 10000000.0;
    private final Set<Material> blacklist = EnumSet.noneOf(Material.class);
    private final Map<String, String> messages = new HashMap<>();

    /** 邮件文案兜底，避免旧配置缺少这些键时邮件只有附件。 */
    private static final Map<String, String> MAIL_DEFAULTS = Map.ofEntries(
            Map.entry("mail-expired-subject", "全球市场商品过期下架"),
            Map.entry("mail-expired-content",
                    "<gray>你的商品已过期自动下架\n"
                            + "<gray>物品：<yellow>%item%</yellow> x<yellow>%amount%</yellow>\n"
                            + "<gray>物品已附在本邮件中，请右键领取。"),
            Map.entry("mail-removed-subject", "全球市场商品已下架"),
            Map.entry("mail-removed-content",
                    "<gray>你的商品已成功下架\n"
                            + "<gray>物品：<yellow>%item%</yellow> x<yellow>%amount%</yellow>\n"
                            + "<gray>物品已附在本邮件中，请右键领取。"),
            Map.entry("mail-force-subject", "管理员下架了你的商品"),
            Map.entry("mail-force-content",
                    "<gray>你的商品被管理员强制下架\n"
                            + "<gray>物品：<yellow>%item%</yellow> x<yellow>%amount%</yellow>\n"
                            + "<gray>物品已附在本邮件中，请右键领取。"),
            Map.entry("mail-bought-subject", "全球市场购买成功"),
            Map.entry("mail-bought-content",
                    "<gray>购买成功\n"
                            + "<gray>物品：<yellow>%item%</yellow> x<yellow>%amount%</yellow>\n"
                            + "<gray>物品已附在本邮件中，请右键领取。"),
            Map.entry("mail-sold-subject", "全球市场商品售出"),
            Map.entry("mail-sold-content",
                    "<gray>你的商品已售出\n"
                            + "<gray>物品：<yellow>%item%</yellow>\n"
                            + "<gray>收入 <yellow>%price%</yellow>（买家：<yellow>%buyer%</yellow>）")
    );

    public MarketConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.dateFormat = config.getString("date-format", "yyyy-MM-dd HH:mm");
        try {
            this.zoneId = ZoneId.of(config.getString("time-zone", "Asia/Shanghai"));
        } catch (Exception e) {
            this.zoneId = ZoneId.of("Asia/Shanghai");
        }
        this.expireDays = Math.max(0, config.getInt("expire-days", 3));
        this.checkInterval = Math.max(10, config.getInt("check-interval", 60));
        this.listingFee = config.getString("listing-fee", "5%");
        this.salesTax = clampPercent(config.getDouble("sales-tax", 0.0));
        this.maxListingsPerPlayer = Math.max(0, config.getInt("max-listings-per-player", 10));
        this.minPrice = Math.max(0, config.getDouble("min-price", 1.0));
        this.maxPrice = Math.max(this.minPrice, config.getDouble("max-price", 10000000.0));

        this.blacklist.clear();
        for (String raw : config.getStringList("blacklisted-materials")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                blacklist.add(Material.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        this.messages.clear();
        this.messages.putAll(MAIL_DEFAULTS);
        ConfigurationSection section = config.getConfigurationSection("messages");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                messages.put(key, section.getString(key, ""));
            }
        }
        // 兼容旧配置：邮件文案若仍写在根节点，也一并读取
        for (String key : MAIL_DEFAULTS.keySet()) {
            String legacy = config.getString(key);
            if (legacy != null && !legacy.isBlank()) {
                messages.put(key, legacy);
            }
        }
    }

    private double clampPercent(double value) {
        return Math.max(0, Math.min(100, value));
    }

    public int expireDays() {
        return expireDays;
    }

    public long expireMillis() {
        return expireDays * 24L * 60 * 60 * 1000;
    }

    public int checkInterval() {
        return checkInterval;
    }

    public double salesTax() {
        return salesTax;
    }

    public int maxListingsPerPlayer() {
        return maxListingsPerPlayer;
    }

    public double minPrice() {
        return minPrice;
    }

    public double maxPrice() {
        return maxPrice;
    }

    public boolean isBlacklisted(Material material) {
        return material != null && blacklist.contains(material);
    }

    public boolean isPercentFee() {
        return listingFee != null && listingFee.trim().endsWith("%");
    }

    public double listingFee(double price) {
        String raw = listingFee == null ? "0" : listingFee.trim();
        try {
            if (raw.endsWith("%")) {
                double percent = Double.parseDouble(raw.substring(0, raw.length() - 1).trim());
                return round(price * percent / 100.0);
            }
            return round(Double.parseDouble(raw));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public String listingFeeDescription() {
        String raw = listingFee == null ? "0" : listingFee.trim();
        if (raw.endsWith("%")) {
            return raw + "（按售价）";
        }
        return raw;
    }

    public double applySalesTax(double amount) {
        if (salesTax <= 0) {
            return round(amount);
        }
        return round(amount * (1.0 - salesTax / 100.0));
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public String formatTime(long millis) {
        if (millis == Long.MAX_VALUE) {
            return "永不过期";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat).withZone(zoneId);
        return fmt.format(Instant.ofEpochMilli(millis));
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
