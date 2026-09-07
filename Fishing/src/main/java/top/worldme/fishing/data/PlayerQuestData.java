package top.worldme.fishing.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerQuestData {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;

    public PlayerQuestData(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 data.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        if (data == null) {
            return;
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 data.yml: " + e.getMessage());
        }
    }

    private String path(UUID uuid, String key) {
        return "players." + uuid + "." + key;
    }

    public String getCycle(UUID uuid) {
        return data.getString(path(uuid, "cycle"), "");
    }

    public void setCycle(UUID uuid, String cycle) {
        data.set(path(uuid, "cycle"), cycle);
    }

    public String getQuestLootId(UUID uuid) {
        return data.getString(path(uuid, "quest-loot-id"), "");
    }

    public void setQuestLootId(UUID uuid, String lootId) {
        data.set(path(uuid, "quest-loot-id"), lootId);
    }

    public boolean isCaught(UUID uuid) {
        return data.getBoolean(path(uuid, "caught"), false);
    }

    public void setCaught(UUID uuid, boolean caught) {
        data.set(path(uuid, "caught"), caught);
    }

    public boolean isCompleted(UUID uuid) {
        return data.getBoolean(path(uuid, "completed"), false);
    }

    public void setCompleted(UUID uuid, boolean completed) {
        data.set(path(uuid, "completed"), completed);
    }

    public int getTotalCompleted(UUID uuid) {
        return data.getInt(path(uuid, "total-completed"), 0);
    }

    public void setTotalCompleted(UUID uuid, int total) {
        data.set(path(uuid, "total-completed"), total);
    }

    public void addTotalCompleted(UUID uuid, int delta) {
        setTotalCompleted(uuid, getTotalCompleted(uuid) + delta);
    }

    public void clear(UUID uuid) {
        data.set("players." + uuid, null);
    }
}
