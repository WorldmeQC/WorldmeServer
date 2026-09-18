package top.worldme.mail.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class MailDatabase {

    private final JavaPlugin plugin;
    private Connection connection;

    public MailDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean open() {
        try {
            Class.forName("org.sqlite.JDBC");
            File file = new File(plugin.getDataFolder(), "mails.db");
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
            st.executeUpdate("CREATE TABLE IF NOT EXISTS mails ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "recipient TEXT NOT NULL,"
                    + "subject TEXT NOT NULL,"
                    + "content TEXT,"
                    + "sent_at INTEGER NOT NULL,"
                    + "expire_at INTEGER NOT NULL,"
                    + "read INTEGER DEFAULT 0,"
                    + "claimed INTEGER DEFAULT 0,"
                    + "items TEXT,"
                    + "commands TEXT"
                    + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("创建 mails 表失败: " + e.getMessage());
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