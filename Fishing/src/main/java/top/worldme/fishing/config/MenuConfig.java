package top.worldme.fishing.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuConfig {

    private final JavaPlugin plugin;
    private final File menuFile;

    private String title = "&6&l渔夫任务";
    private int rows = 3;
    private final List<Integer> decorationSlots = new ArrayList<>();
    private ItemConfig decorationItem = new ItemConfig();
    private int questInfoSlot = 11;
    private ItemConfig questInfoFallback = new ItemConfig();
    private String questInfoName = "&b今日任务鱼: &e%fish_name%";
    private final List<String> questInfoLore = new ArrayList<>();
    private int completeSlot = 15;
    private ItemConfig completeReady = new ItemConfig();
    private ItemConfig completeNotReady = new ItemConfig();

    public MenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menuFile = new File(plugin.getDataFolder(), "menu.yml");
        load();
    }

    public void load() {
        if (!menuFile.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(menuFile);

        this.title = config.getString("title", "&6&l渔夫任务");
        this.rows = Math.max(1, Math.min(6, config.getInt("rows", 3)));

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("menu.yml 中未找到 items 节点。");
            return;
        }

        ConfigurationSection decorationSection = itemsSection.getConfigurationSection("decoration");
        if (decorationSection != null) {
            this.decorationItem = readItemConfig(decorationSection);
            this.decorationSlots.clear();
            this.decorationSlots.addAll(decorationSection.getIntegerList("slots"));
        }

        ConfigurationSection questInfoSection = itemsSection.getConfigurationSection("quest-info");
        if (questInfoSection != null) {
            this.questInfoSlot = questInfoSection.getInt("slot", 11);
            this.questInfoFallback = readItemConfig(questInfoSection, "fallback-");
            this.questInfoName = questInfoSection.getString("name", this.questInfoName);
            this.questInfoLore.clear();
            this.questInfoLore.addAll(questInfoSection.getStringList("lore"));
        }

        ConfigurationSection completeSection = itemsSection.getConfigurationSection("complete-button");
        if (completeSection != null) {
            this.completeSlot = completeSection.getInt("slot", 15);
            ConfigurationSection readySection = completeSection.getConfigurationSection("ready");
            if (readySection != null) {
                this.completeReady = readItemConfig(readySection);
            }
            ConfigurationSection notReadySection = completeSection.getConfigurationSection("not-ready");
            if (notReadySection != null) {
                this.completeNotReady = readItemConfig(notReadySection);
            }
        }
    }

    private ItemConfig readItemConfig(ConfigurationSection section) {
        return readItemConfig(section, "");
    }

    private ItemConfig readItemConfig(ConfigurationSection section, String prefix) {
        ItemConfig item = new ItemConfig();
        item.material = section.getString(prefix + "material", null);
        item.name = section.getString(prefix + "name", null);
        item.lore.clear();
        item.lore.addAll(section.getStringList(prefix + "lore"));
        return item;
    }

    public String title() {
        return title;
    }

    public int rows() {
        return rows;
    }

    public int size() {
        return rows * 9;
    }

    public List<Integer> decorationSlots() {
        return Collections.unmodifiableList(decorationSlots);
    }

    public ItemConfig decorationItem() {
        return decorationItem;
    }

    public int questInfoSlot() {
        return questInfoSlot;
    }

    public ItemConfig questInfoFallback() {
        return questInfoFallback;
    }

    public String questInfoName() {
        return questInfoName;
    }

    public List<String> questInfoLore() {
        return Collections.unmodifiableList(questInfoLore);
    }

    public int completeSlot() {
        return completeSlot;
    }

    public ItemConfig completeReady() {
        return completeReady;
    }

    public ItemConfig completeNotReady() {
        return completeNotReady;
    }

    public static class ItemConfig {
        public String material;
        public String name;
        public final List<String> lore = new ArrayList<>();
    }
}
