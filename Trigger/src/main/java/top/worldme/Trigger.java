package top.worldme;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.trigger.command.TriggerCommand;
import top.worldme.trigger.config.TriggerConfig;
import top.worldme.trigger.data.PlayerUnlockData;
import top.worldme.trigger.listener.PlayerDimensionListener;
import top.worldme.trigger.listener.StructureGenerateListener;

public class Trigger extends JavaPlugin {

    private static Trigger instance;
    private TriggerConfig triggerConfig;
    private PlayerUnlockData playerUnlockData;
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.triggerConfig = new TriggerConfig(this);
        this.playerUnlockData = new PlayerUnlockData(this);

        loadLuckPerms();

        Bukkit.getPluginManager().registerEvents(
                new PlayerDimensionListener(this, triggerConfig, playerUnlockData, luckPerms),
                this
        );

        Bukkit.getPluginManager().registerEvents(
                new StructureGenerateListener(),
                this
        );

        getCommand("worldmetrigger").setExecutor(new TriggerCommand(this, triggerConfig));

        getLogger().info("Worldme-Trigger 已加载。");
    }

    @Override
    public void onDisable() {
        if (playerUnlockData != null) {
            playerUnlockData.save();
        }
        getLogger().info("Worldme-Trigger 已卸载。");
    }

    private void loadLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().warning("未检测到 LuckPerms，触发器将不会授予权限。");
            return;
        }
        this.luckPerms = provider.getProvider();
    }

    public static Trigger getInstance() {
        return instance;
    }

    public TriggerConfig getTriggerConfig() {
        return triggerConfig;
    }

    public PlayerUnlockData getPlayerUnlockData() {
        return playerUnlockData;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
