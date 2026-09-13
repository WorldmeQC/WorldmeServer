package top.worldme.ownership.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BindMenuConfig {

    private final JavaPlugin plugin;
    private final File menuFile;

    private String title = "<gold><bold>物品认主";
    private int rows = 3;

    private ItemConfig decoration = new ItemConfig();
    private final List<Integer> decorationSlots = new ArrayList<>();

    private int inputSlot = 13;

    private ItemConfig info = new ItemConfig();
    private int infoSlot = 11;

    private ItemConfig confirm = new ItemConfig();
    private int confirmSlot = 15;

    public BindMenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menuFile = new File(plugin.getDataFolder(), "menu.yml");
        load();
    }

    public void load() {
        if (!menuFile.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(menuFile);

        this.title = config.getString("title", this.title);
        this.rows = Math.max(1, Math.min(6, config.getInt("rows", 3)));
        this.inputSlot = config.getInt("items.input-slot", 13);

        ConfigurationSection items = config.getConfigurationSection("items");
        if (items == null) {
            plugin.getLogger().warning("menu.yml 中未找到 items 节点。");
            return;
        }

        ConfigurationSection decorationSection = items.getConfigurationSection("decoration");
        if (decorationSection != null) {
            this.decoration = readItemConfig(decorationSection);
            this.decorationSlots.clear();
            this.decorationSlots.addAll(decorationSection.getIntegerList("slots"));
        }

        ConfigurationSection infoSection = items.getConfigurationSection("info");
        if (infoSection != null) {
            this.infoSlot = infoSection.getInt("slot", this.infoSlot);
            this.info = readItemConfig(infoSection);
        }

        ConfigurationSection confirmSection = items.getConfigurationSection("confirm-button");
        if (confirmSection != null) {
            this.confirmSlot = confirmSection.getInt("slot", this.confirmSlot);
            this.confirm = readItemConfig(confirmSection);
        }
    }

    private ItemConfig readItemConfig(ConfigurationSection section) {
        ItemConfig item = new ItemConfig();
        item.material = section.getString("material", null);
        item.name = section.getString("name", null);
        item.lore.clear();
        item.lore.addAll(section.getStringList("lore"));
        return item;
    }

    public String title() {
        return title;
    }

    public int size() {
        return rows * 9;
    }

    public ItemConfig decoration() {
        return decoration;
    }

    public List<Integer> decorationSlots() {
        return Collections.unmodifiableList(decorationSlots);
    }

    public int inputSlot() {
        return inputSlot;
    }

    public ItemConfig info() {
        return info;
    }

    public int infoSlot() {
        return infoSlot;
    }

    public ItemConfig confirm() {
        return confirm;
    }

    public int confirmSlot() {
        return confirmSlot;
    }

    public static class ItemConfig {
        public String material;
        public String name;
        public final List<String> lore = new ArrayList<>();
    }
}
