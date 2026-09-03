package top.worldme.trigger.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerUnlockData {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;

    public PlayerUnlockData(JavaPlugin plugin) {
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

    public boolean isUnlocked(UUID uuid, String worldKey) {
        return data.getBoolean("unlocked." + uuid + "." + worldKey, false);
    }

    public void setUnlocked(UUID uuid, String worldKey) {
        data.set("unlocked." + uuid + "." + worldKey, true);
        save();
    }
}
