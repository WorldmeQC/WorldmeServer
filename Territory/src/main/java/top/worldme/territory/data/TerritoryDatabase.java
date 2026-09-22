package top.worldme.territory.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TerritoryDatabase {

    private final JavaPlugin plugin;
    private Connection connection;

    public TerritoryDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean open() {
        try {
            Class.forName("org.sqlite.JDBC");
            File file = new File(plugin.getDataFolder(), "territory.db");
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
            st.executeUpdate("CREATE TABLE IF NOT EXISTS regions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "owner_type TEXT NOT NULL,"
                    + "owner_id TEXT NOT NULL,"
                    + "name TEXT NOT NULL,"
                    + "world TEXT NOT NULL,"
                    + "min_chunk_x INTEGER NOT NULL,"
                    + "min_section_y INTEGER NOT NULL,"
                    + "min_chunk_z INTEGER NOT NULL,"
                    + "chunks_x INTEGER NOT NULL,"
                    + "sections_y INTEGER NOT NULL,"
                    + "chunks_z INTEGER NOT NULL,"
                    + "flags INTEGER NOT NULL DEFAULT 0,"
                    + "has_warp INTEGER NOT NULL DEFAULT 0,"
                    + "warp_x REAL NOT NULL DEFAULT 0,"
                    + "warp_y REAL NOT NULL DEFAULT 0,"
                    + "warp_z REAL NOT NULL DEFAULT 0,"
                    + "warp_yaw REAL NOT NULL DEFAULT 0,"
                    + "warp_pitch REAL NOT NULL DEFAULT 0,"
                    + "enter_msg TEXT,"
                    + "leave_msg TEXT,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS region_members ("
                    + "region_id INTEGER NOT NULL,"
                    + "player_uuid TEXT NOT NULL,"
                    + "role TEXT NOT NULL,"
                    + "permissions INTEGER NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY(region_id, player_uuid)"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS sub_regions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "region_id INTEGER NOT NULL,"
                    + "name TEXT NOT NULL,"
                    + "min_offset_x INTEGER NOT NULL,"
                    + "min_offset_y INTEGER NOT NULL,"
                    + "min_offset_z INTEGER NOT NULL,"
                    + "chunks_x INTEGER NOT NULL,"
                    + "sections_y INTEGER NOT NULL,"
                    + "chunks_z INTEGER NOT NULL,"
                    + "flags INTEGER NOT NULL DEFAULT 0,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS sub_region_members ("
                    + "sub_region_id INTEGER NOT NULL,"
                    + "player_uuid TEXT NOT NULL,"
                    + "role TEXT NOT NULL,"
                    + "permissions INTEGER NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY(sub_region_id, player_uuid)"
                    + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("创建领地数据表失败: " + e.getMessage());
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
