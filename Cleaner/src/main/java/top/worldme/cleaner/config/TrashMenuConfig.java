package top.worldme.cleaner.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrashMenuConfig {

    private final JavaPlugin plugin;
    private final File menuFile;

    private String title = "<gold><bold>垃圾桶";
    private int rows = 4;

    private ItemConfig decoration = new ItemConfig();
    private final List<Integer> decorationSlots = new ArrayList<>();

    private final List<Integer> contentSlots = new ArrayList<>();

    private ItemConfig info = new ItemConfig();
    private int infoSlot = 4;

    private ItemConfig prevButton = new ItemConfig();
    private int prevSlot = 18;

    private ItemConfig nextButton = new ItemConfig();
    private int nextSlot = 26;

    public TrashMenuConfig(JavaPlugin plugin) {
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
        this.rows = Math.max(1, Math.min(6, config.getInt("rows", 4)));

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

        this.contentSlots.clear();
        this.contentSlots.addAll(items.getIntegerList("content-slots"));

        ConfigurationSection infoSection = items.getConfigurationSection("info");
        if (infoSection != null) {
            this.infoSlot = infoSection.getInt("slot", this.infoSlot);
            this.info = readItemConfig(infoSection);
        }

        ConfigurationSection prevSection = items.getConfigurationSection("prev-button");
        if (prevSection != null) {
            this.prevSlot = prevSection.getInt("slot", this.prevSlot);
            this.prevButton = readItemConfig(prevSection);
        }

        ConfigurationSection nextSection = items.getConfigurationSection("next-button");
        if (nextSection != null) {
            this.nextSlot = nextSection.getInt("slot", this.nextSlot);
            this.nextButton = readItemConfig(nextSection);
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

    public List<Integer> contentSlots() {
        return Collections.unmodifiableList(contentSlots);
    }

    public ItemConfig info() {
        return info;
    }

    public int infoSlot() {
        return infoSlot;
    }

    public ItemConfig prevButton() {
        return prevButton;
    }

    public int prevSlot() {
        return prevSlot;
    }

    public ItemConfig nextButton() {
        return nextButton;
    }

    public int nextSlot() {
        return nextSlot;
    }

    public static class ItemConfig {
        public String material;
        public String name;
        public final List<String> lore = new ArrayList<>();
    }
}