package top.worldme.market.manager;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.market.bridge.MailBridge;
import top.worldme.market.config.MarketConfig;
import top.worldme.market.data.MarketDatabase;
import top.worldme.market.data.MarketItems;
import top.worldme.market.data.MarketListing;
import top.worldme.market.economy.EconomyHook;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class MarketManager {

    private final JavaPlugin plugin;
    private final MarketConfig config;
    private final MarketDatabase database;
    private final EconomyHook economy;
    private final MailBridge mail;
    private final List<MarketListing> listings = new CopyOnWriteArrayList<>();

    public MarketManager(JavaPlugin plugin, MarketConfig config, MarketDatabase database,
                         EconomyHook economy, MailBridge mail) {
        this.plugin = plugin;
        this.config = config;
        this.database = database;
        this.economy = economy;
        this.mail = mail;
    }

    // ---------- 加载 / 过期 ----------

    public void load() {
        listings.clear();
        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, seller, item_data, amount, price, listed_at, expire_at FROM market_listings")) {
            while (rs.next()) {
                ItemStack item = MarketItems.deserialize(rs.getString("item_data"));
                if (item == null) {
                    continue;
                }
                listings.add(new MarketListing(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("seller")),
                        item,
                        rs.getInt("amount"),
                        rs.getDouble("price"),
                        rs.getLong("listed_at"),
                        rs.getLong("expire_at")));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载全球市场失败: " + e.getMessage());
        }
        deleteExpired();
        plugin.getLogger().info("全球市场加载完成：" + listings.size() + " 个商品。");
    }

    public void deleteExpired() {
        if (config.expireDays() <= 0 || listings.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<MarketListing> expired = new ArrayList<>();
        for (MarketListing listing : listings) {
            if (listing.expireAt() <= now) {
                expired.add(listing);
            }
        }
        if (expired.isEmpty()) {
            return;
        }
        if (!mail.isReady()) {
            plugin.getLogger().warning("邮箱不可用，暂不处理 " + expired.size() + " 个过期商品。");
            return;
        }
        int done = 0;
        for (MarketListing listing : expired) {
            if (deliver(listing, "mail-expired-subject", "mail-expired-content", null)) {
                delete(listing);
                done++;
            }
        }
        if (done > 0) {
            plugin.getLogger().info("已自动下架 " + done + " 个过期商品。");
        }
    }

    // ---------- 查询 ----------

    public List<MarketListing> getAll() {
        return new ArrayList<>(listings);
    }

    public List<MarketListing> getBySeller(UUID seller) {
        List<MarketListing> result = new ArrayList<>();
        for (MarketListing listing : listings) {
            if (listing.seller().equals(seller)) {
                result.add(listing);
            }
        }
        return result;
    }

    public MarketListing getById(int id) {
        for (MarketListing listing : listings) {
            if (listing.id() == id) {
                return listing;
            }
        }
        return null;
    }

    public int count() {
        return listings.size();
    }

    public int countBySeller(UUID seller) {
        int count = 0;
        for (MarketListing listing : listings) {
            if (listing.seller().equals(seller)) {
                count++;
            }
        }
        return count;
    }

    public List<MarketListing> getAllPaged(int page, int pageSize) {
        return page(listings, page, pageSize);
    }

    public List<MarketListing> getBySellerPaged(UUID seller, int page, int pageSize) {
        return page(getBySeller(seller), page, pageSize);
    }

    private List<MarketListing> page(List<MarketListing> source, int page, int pageSize) {
        if (pageSize <= 0 || source.isEmpty()) {
            return Collections.emptyList();
        }
        int from = (Math.max(1, page) - 1) * pageSize;
        if (from >= source.size()) {
            return Collections.emptyList();
        }
        int to = Math.min(from + pageSize, source.size());
        return new ArrayList<>(source.subList(from, to));
    }

    public int maxPage(int total, int pageSize) {
        if (pageSize <= 0 || total <= 0) {
            return 1;
        }
        return Math.max(1, (total + pageSize - 1) / pageSize);
    }

    // ---------- 上架 ----------

    public MarketResult create(Player seller, ItemStack item, double price) {
        if (item == null || item.getType().isAir()) {
            return MarketResult.NO_ITEM;
        }
        if (config.isBlacklisted(item.getType())) {
            return MarketResult.BLACKLISTED;
        }
        if (price < config.minPrice() || price > config.maxPrice()) {
            return MarketResult.PRICE_INVALID;
        }
        if (config.maxListingsPerPlayer() > 0 && countBySeller(seller.getUniqueId()) >= config.maxListingsPerPlayer()) {
            return MarketResult.TOO_MANY;
        }
        if (!economy.isReady()) {
            return MarketResult.ECONOMY_ERROR;
        }

        double fee = config.listingFee(price);
        if (fee > 0 && !economy.withdraw(seller.getUniqueId(), fee)) {
            return MarketResult.NO_MONEY;
        }

        long now = System.currentTimeMillis();
        long expire = config.expireDays() > 0 ? now + config.expireMillis() : Long.MAX_VALUE;
        ItemStack stored = item.clone();
        String data = MarketItems.serialize(stored);
        if (data == null) {
            if (fee > 0) {
                economy.deposit(seller.getUniqueId(), fee);
            }
            return MarketResult.NO_ITEM;
        }

        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO market_listings(seller, item_data, amount, price, listed_at, expire_at) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, seller.getUniqueId().toString());
            ps.setString(2, data);
            ps.setInt(3, stored.getAmount());
            ps.setDouble(4, MarketConfig.round(price));
            ps.setLong(5, now);
            ps.setLong(6, expire);
            ps.executeUpdate();

            int id = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }
            listings.add(new MarketListing(id, seller.getUniqueId(), stored,
                    stored.getAmount(), MarketConfig.round(price), now, expire));
            return MarketResult.OK;
        } catch (Exception e) {
            plugin.getLogger().warning("上架失败: " + e.getMessage());
            if (fee > 0) {
                economy.deposit(seller.getUniqueId(), fee);
            }
            return MarketResult.ERROR;
        }
    }

    // ---------- 购买 ----------

    public MarketResult buy(Player buyer, MarketListing listing) {
        if (!listings.contains(listing)) {
            return MarketResult.SALED;
        }
        if (buyer.getUniqueId().equals(listing.seller())) {
            return MarketResult.IS_MINE;
        }
        if (!mail.isReady()) {
            return MarketResult.MAIL_ERROR;
        }
        if (!economy.isReady() || !economy.has(buyer.getUniqueId(), listing.price())) {
            return MarketResult.NO_MONEY;
        }
        if (!economy.withdraw(buyer.getUniqueId(), listing.price())) {
            return MarketResult.NO_MONEY;
        }

        double payout = config.applySalesTax(listing.price());
        if (payout > 0 && !economy.deposit(listing.seller(), payout)) {
            economy.deposit(buyer.getUniqueId(), listing.price());
            return MarketResult.ERROR;
        }

        delete(listing);
        ItemStack item = itemOf(listing);
        boolean mailed = mail.send(buyer.getUniqueId(), subject("mail-bought-subject"),
                content("mail-bought-content", item, listing, buyer.getName()), List.of(item));
        if (!mailed) {
            economy.deposit(buyer.getUniqueId(), listing.price());
            if (payout > 0) {
                economy.withdraw(listing.seller(), payout);
            }
            restore(listing);
            return MarketResult.MAIL_ERROR;
        }

        notifySellerOfSale(listing, buyer.getName(), payout);
        return MarketResult.OK;
    }

    // ---------- 下架 ----------

    public MarketResult remove(Player seller, MarketListing listing) {
        if (!listings.contains(listing)) {
            return MarketResult.SALED;
        }
        if (!seller.getUniqueId().equals(listing.seller())) {
            return MarketResult.NOT_MINE;
        }
        if (!mail.isReady()) {
            return MarketResult.MAIL_ERROR;
        }
        if (!deliver(listing, "mail-removed-subject", "mail-removed-content", null)) {
            return MarketResult.MAIL_ERROR;
        }
        delete(listing);
        return MarketResult.OK;
    }

    public MarketResult forceRemove(MarketListing listing) {
        if (!listings.contains(listing)) {
            return MarketResult.SALED;
        }
        if (!mail.isReady()) {
            return MarketResult.MAIL_ERROR;
        }
        if (!deliver(listing, "mail-force-subject", "mail-force-content", null)) {
            return MarketResult.MAIL_ERROR;
        }
        delete(listing);
        return MarketResult.OK;
    }

    // ---------- 内部 ----------

    private void delete(MarketListing listing) {
        listings.remove(listing);
        exec("DELETE FROM market_listings WHERE id=" + listing.id());
    }

    private void restore(MarketListing listing) {
        listings.add(listing);
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO market_listings(id, seller, item_data, amount, price, listed_at, expire_at) VALUES(?,?,?,?,?,?,?)")) {
            ps.setInt(1, listing.id());
            ps.setString(2, listing.seller().toString());
            ps.setString(3, MarketItems.serialize(listing.item()));
            ps.setInt(4, listing.amount());
            ps.setDouble(5, listing.price());
            ps.setLong(6, listing.listedAt());
            ps.setLong(7, listing.expireAt());
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void exec(String sql) {
        try (Statement st = database.getConnection().createStatement()) {
            st.executeUpdate(sql);
        } catch (Exception e) {
            plugin.getLogger().warning("市场 SQL 失败: " + e.getMessage());
        }
    }

    private ItemStack itemOf(MarketListing listing) {
        ItemStack item = listing.item().clone();
        item.setAmount(Math.max(1, listing.amount()));
        return item;
    }

    private boolean deliver(MarketListing listing, String subjectKey, String contentKey, String extra) {
        ItemStack item = itemOf(listing);
        return mail.send(listing.seller(), subject(subjectKey),
                content(contentKey, item, listing, extra), List.of(item));
    }

    private void notifySellerOfSale(MarketListing listing, String buyerName, double payout) {
        String item = displayName(itemOf(listing));
        mail.send(listing.seller(), subject("mail-sold-subject"),
                config.getMessage("mail-sold-content", Map.of(
                        "item", item,
                        "price", economy.format(payout),
                        "buyer", buyerName == null ? "" : buyerName)),
                null);
    }

    private String subject(String key) {
        return config.getMessage(key);
    }

    private String content(String key, ItemStack item, MarketListing listing, String extra) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", displayName(item));
        placeholders.put("amount", String.valueOf(listing.amount()));
        placeholders.put("price", economy.format(listing.price()));
        String sellerName = Bukkit.getOfflinePlayer(listing.seller()).getName();
        placeholders.put("seller", sellerName == null ? listing.seller().toString() : sellerName);
        if (extra != null) {
            placeholders.put("buyer", extra);
        }
        return config.getMessage(key, placeholders);
    }

    public static String displayName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        return item.getType().name();
    }

    public enum MarketResult {
        OK,
        NO_ITEM,
        BLACKLISTED,
        PRICE_INVALID,
        TOO_MANY,
        NO_MONEY,
        IS_MINE,
        NOT_MINE,
        SALED,
        MAIL_ERROR,
        ECONOMY_ERROR,
        ERROR
    }
}
