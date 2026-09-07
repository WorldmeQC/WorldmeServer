package top.worldme;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.action.ActionManager;
import net.momirealms.customfishing.api.mechanic.context.ContextKeys;
import net.momirealms.customfishing.api.mechanic.requirement.RequirementFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        Bukkit.getScheduler().runTask(this, this::registerCustomRequirement);
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

    public void registerCustomRequirement() {
        if (Bukkit.getPluginManager().getPlugin("CustomFishing") == null) {
            this.getLogger().warning("未检测到 CustomFishing，任务鱼可钓控制将不会生效。");
            return;
        }
        try {
            BukkitCustomFishingPlugin api = BukkitCustomFishingPlugin.getInstance();
            RequirementFactory<Player> factory = (args, actions, runActions) -> context -> {
                Player player = context.holder();
                if (player == null) {
                    return false;
                }
                String lootId = context.arg(ContextKeys.ID);
                if (lootId == null) {
                    return false;
                }
                if (questManager.isQuestActiveForLoot(player, lootId)) {
                    return true;
                }
                if (runActions && !actions.isEmpty()) {
                    ActionManager.trigger(context, actions);
                }
                return false;
            };
            boolean registered = api.getRequirementManager().registerRequirement(factory, "fisherman_quest");
            if (registered) {
                this.getLogger().info("已向 CustomFishing 注册 fisherman_quest 条件。");
            }
        } catch (Exception e) {
            this.getLogger().severe("注册 CustomFishing 自定义条件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
