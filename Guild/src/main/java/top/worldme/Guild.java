package top.worldme;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.guild.command.GuildCommand;
import top.worldme.guild.config.GuildConfig;
import top.worldme.guild.config.GuildMenuConfig;
import top.worldme.guild.config.StructureConfig;
import top.worldme.guild.data.GuildDatabase;
import top.worldme.guild.economy.VaultHook;
import top.worldme.guild.feature.impl.BeaconFeature;
import top.worldme.guild.feature.impl.WarehouseFeature;
import top.worldme.guild.gui.GuildMenuListener;
import top.worldme.guild.listener.GuildListener;
import top.worldme.guild.manager.GuildManager;
import top.worldme.guild.papi.GuildPlaceholders;
import top.worldme.territory.api.TerritoryApi;

public class Guild extends JavaPlugin {

    private static Guild instance;
    private GuildConfig guildConfig;
    private GuildMenuConfig menuConfig;
    private StructureConfig structureConfig;
    private GuildDatabase database;
    private VaultHook vaultHook;
    private GuildManager guildManager;
    private TerritoryApi territoryApi;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("menu.yml", false);
        saveResource("structures.yml", false);

        this.guildConfig = new GuildConfig(this);
        this.menuConfig = new GuildMenuConfig(this);
        this.structureConfig = new StructureConfig(this);

        this.vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            getLogger().warning("未检测到 Vault 经济系统，公会资金功能将不可用。");
        }

        this.database = new GuildDatabase(this);
        if (!database.open()) {
            getLogger().severe("数据库初始化失败，插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Plugin territoryPlugin = Bukkit.getPluginManager().getPlugin("Worldme-Territory");
        if (territoryPlugin instanceof Territory territory) {
            this.territoryApi = territory.getApi();
        } else {
            getLogger().severe("未检测到 Worldme-Territory，插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.guildManager = new GuildManager(this, guildConfig, structureConfig, database, vaultHook, territoryApi);
        guildManager.load();

        guildManager.registerFeature(new BeaconFeature(this));
        guildManager.registerFeature(new WarehouseFeature(this));
        guildManager.scanAll();
        Bukkit.getScheduler().runTaskTimer(this, () -> guildManager.tickFeatures(), 200L, 100L);

        Bukkit.getPluginManager().registerEvents(new GuildMenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new GuildListener(guildManager), this);

        GuildCommand command = new GuildCommand(this, guildConfig, guildManager);
        getCommand("wmguild").setExecutor(command);
        getCommand("wmguild").setTabCompleter(command);

        registerPlaceholders();

        getLogger().info("Worldme-Guild 已加载。");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("Worldme-Guild 已卸载。");
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new GuildPlaceholders(this).register();
            getLogger().info("已注册 PlaceholderAPI 占位符。");
        } catch (Throwable t) {
            getLogger().warning("注册 PlaceholderAPI 占位符失败: " + t.getMessage());
        }
    }

    public static Guild getInstance() {
        return instance;
    }

    public GuildConfig getGuildConfig() {
        return guildConfig;
    }

    public GuildMenuConfig getMenuConfig() {
        return menuConfig;
    }

    public StructureConfig getStructureConfig() {
        return structureConfig;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    /**
     * 供其他模块调用的公会接口。
     */
    public top.worldme.guild.api.GuildApi getApi() {
        return guildManager;
    }
}
