package top.worldme;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.fishing.command.FishingCommand;
import top.worldme.fishing.config.FishingConfig;
import top.worldme.fishing.config.MenuConfig;
import top.worldme.fishing.data.PlayerQuestData;
import top.worldme.fishing.gui.QuestGui;
import top.worldme.fishing.listener.FishingListener;
import top.worldme.fishing.quest.QuestManager;
import top.worldme.fishing.util.QuestKeys;

public class Fishing extends JavaPlugin {

    private static Fishing instance;
    private FishingConfig fishingConfig;
    private MenuConfig menuConfig;
    private PlayerQuestData playerQuestData;
    private QuestKeys questKeys;
    private QuestManager questManager;
    private FishingListener fishingListener;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("menu.yml", false);

        this.fishingConfig = new FishingConfig(this);
        this.menuConfig = new MenuConfig(this);
        this.playerQuestData = new PlayerQuestData(this);
        this.questKeys = new QuestKeys(this);
        this.questManager = new QuestManager(fishingConfig, playerQuestData, questKeys);

        this.fishingListener = new FishingListener(this, questManager, questKeys);
        Bukkit.getPluginManager().registerEvents(fishingListener, this);
        Bukkit.getPluginManager().registerEvents(new QuestGui.GuiListener(questManager, menuConfig), this);

        FishingCommand fishingCommand = new FishingCommand(this, questManager, menuConfig, fishingConfig);
        getCommand("wmfishing").setExecutor(fishingCommand);
        getCommand("wmfishing").setTabCompleter(fishingCommand);

        getLogger().info("Worldme-Fishing 已加载。");
    }

    @Override
    public void onDisable() {
        if (playerQuestData != null) {
            playerQuestData.save();
        }
        getLogger().info("Worldme-Fishing 已卸载。");
    }

    public static Fishing getInstance() {
        return instance;
    }

    public FishingConfig getFishingConfig() {
        return fishingConfig;
    }

    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

    public PlayerQuestData getPlayerQuestData() {
        return playerQuestData;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }
}
