package top.worldme.market.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class MarketDatabase {

    private final JavaPlugin plugin;
    private Connection connection;

    public MarketDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean open() {
        try {
            Class.forName("org.sqlite.JDBC");
            File file = new File(plugin.getDataFolder(), "market.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            createTable();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("打开 SQLite 数据库失败: " + e.getMessage());
            return false;
        }
    }

    private void createTable() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS market_listings ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "seller TEXT NOT NULL,"
                    + "item_data TEXT NOT NULL,"
                    + "amount INTEGER NOT NULL,"
                    + "price REAL NOT NULL,"
                    + "listed_at INTEGER NOT NULL,"
                    + "expire_at INTEGER NOT NULL"
                    + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("创建 market_listings 表失败: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("关闭数据库失败: " + e.getMessage());
        }
    }
}
