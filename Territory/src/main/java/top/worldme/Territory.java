package top.worldme;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.territory.api.TerritoryApi;
import top.worldme.territory.command.TerritoryCommand;
import top.worldme.territory.config.TerritoryConfig;
import top.worldme.territory.config.TerritoryMenuConfig;
import top.worldme.territory.data.TerritoryDatabase;
import top.worldme.territory.economy.VaultHook;
import top.worldme.territory.gui.TerritoryMenuListener;
import top.worldme.territory.listener.ProtectionListener;
import top.worldme.territory.listener.RegionTracker;
import top.worldme.territory.manager.RegionManager;
import top.worldme.territory.papi.TerritoryPlaceholders;

public class Territory extends JavaPlugin {

    private static Territory instance;
    private TerritoryConfig territoryConfig;
    private TerritoryMenuConfig menuConfig;
    private TerritoryDatabase database;
    private VaultHook vaultHook;
    private RegionManager regionManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("menu.yml", false);

        this.territoryConfig = new TerritoryConfig(this);
        this.menuConfig = new TerritoryMenuConfig(this);

        this.vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            getLogger().warning("未检测到 Vault 经济系统，圈地与扩展将不可用。");
        }

        this.database = new TerritoryDatabase(this);
        if (!database.open()) {
            getLogger().severe("数据库初始化失败，插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.regionManager = new RegionManager(this, territoryConfig, database, vaultHook);
        regionManager.load();

        Bukkit.getPluginManager().registerEvents(new RegionTracker(territoryConfig, regionManager), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(territoryConfig, regionManager), this);
        Bukkit.getPluginManager().registerEvents(new TerritoryMenuListener(), this);

        TerritoryCommand command = new TerritoryCommand(this, territoryConfig, regionManager);
        getCommand("wmterritory").setExecutor(command);
        getCommand("wmterritory").setTabCompleter(command);

        registerPlaceholders();

        getLogger().info("Worldme-Territory 已加载。");
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new TerritoryPlaceholders(this).register();
            getLogger().info("已注册 PlaceholderAPI 占位符。");
        } catch (Throwable t) {
            getLogger().warning("注册 PlaceholderAPI 占位符失败: " + t.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("Worldme-Territory 已卸载。");
    }

    public static Territory getInstance() {
        return instance;
    }

    public TerritoryConfig getTerritoryConfig() {
        return territoryConfig;
    }

    public TerritoryMenuConfig getMenuConfig() {
        return menuConfig;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    /**
     * 供其他模块调用的领地接口。
     */
    public TerritoryApi getApi() {
        return regionManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }
}
