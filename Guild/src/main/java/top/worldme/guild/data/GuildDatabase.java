package top.worldme.guild.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class GuildDatabase {

    private final JavaPlugin plugin;
    private Connection connection;

    public GuildDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean open() {
        try {
            Class.forName("org.sqlite.JDBC");
            File file = new File(plugin.getDataFolder(), "guild.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("打开 SQLite 数据库失败: " + e.getMessage());
            return false;
        }
    }

    private void createTables() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS guilds ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "tag TEXT,"
                    + "leader TEXT NOT NULL,"
                    + "balance REAL NOT NULL DEFAULT 0,"
                    + "join_fee REAL NOT NULL DEFAULT 0,"
                    + "open_join INTEGER NOT NULL DEFAULT 0,"
                    + "level INTEGER NOT NULL DEFAULT 1,"
                    + "exp INTEGER NOT NULL DEFAULT 0,"
                    + "region_id INTEGER NOT NULL DEFAULT -1,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS guild_members ("
                    + "guild_id INTEGER NOT NULL,"
                    + "player_uuid TEXT NOT NULL,"
                    + "rank_key TEXT NOT NULL,"
                    + "joined_at INTEGER NOT NULL,"
                    + "fee_paid REAL NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY(guild_id, player_uuid)"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS guild_ranks ("
                    + "guild_id INTEGER NOT NULL,"
                    + "rank_key TEXT NOT NULL,"
                    + "display TEXT NOT NULL,"
                    + "priority INTEGER NOT NULL DEFAULT 0,"
                    + "permissions INTEGER NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY(guild_id, rank_key)"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS guild_features ("
                    + "guild_id INTEGER NOT NULL,"
                    + "feature_key TEXT NOT NULL,"
                    + "structure_key TEXT,"
                    + "unlocked INTEGER NOT NULL DEFAULT 0,"
                    + "enabled INTEGER NOT NULL DEFAULT 0,"
                    + "data TEXT,"
                    + "unlocked_at INTEGER NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY(guild_id, feature_key)"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS guild_fund_requests ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "guild_id INTEGER NOT NULL,"
                    + "uuid TEXT NOT NULL,"
                    + "amount REAL NOT NULL,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS guild_join_requests ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "guild_id INTEGER NOT NULL,"
                    + "uuid TEXT NOT NULL,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS guild_notices ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "uuid TEXT NOT NULL,"
                    + "message TEXT NOT NULL,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS guild_quit ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "leave_at INTEGER NOT NULL"
                    + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("创建公会数据表失败: " + e.getMessage());
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
