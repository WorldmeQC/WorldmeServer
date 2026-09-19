package top.worldme;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.market.bridge.MailBridge;
import top.worldme.market.command.MarketCommand;
import top.worldme.market.config.MarketConfig;
import top.worldme.market.config.MarketMenuConfig;
import top.worldme.market.data.MarketDatabase;
import top.worldme.market.economy.EconomyHook;
import top.worldme.market.gui.MarketMenuListener;
import top.worldme.market.manager.MarketManager;

public class Market extends JavaPlugin {

    private static Market instance;
    private MarketConfig marketConfig;
    private MarketMenuConfig menuConfig;
    private MarketDatabase database;
    private EconomyHook economy;
    private MailBridge mail;
    private MarketManager marketManager;
    private int expireTaskId = -1;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("menu.yml", false);

        this.marketConfig = new MarketConfig(this);
        this.menuConfig = new MarketMenuConfig(this);

        this.database = new MarketDatabase(this);
        if (!database.open()) {
            getLogger().severe("数据库初始化失败，插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.economy = new EconomyHook(this);
        if (!economy.setup()) {
            getLogger().severe("未检测到 Vault 经济系统，插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.mail = new MailBridge(this);
        if (!mail.setup()) {
            getLogger().severe("未检测到 Worldme-Mail，插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.marketManager = new MarketManager(this, marketConfig, database, economy, mail);
        this.marketManager.load();

        Bukkit.getPluginManager().registerEvents(new MarketMenuListener(this), this);
        startExpireTask();

        MarketCommand command = new MarketCommand(this);
        getCommand("wmmarket").setExecutor(command);
        getCommand("wmmarket").setTabCompleter(command);

        getLogger().info("Worldme-Market 已加载。");
    }

    @Override
    public void onDisable() {
        if (expireTaskId != -1) {
            Bukkit.getScheduler().cancelTask(expireTaskId);
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("Worldme-Market 已卸载。");
    }

    private void startExpireTask() {
        long ticks = marketConfig.checkInterval() * 20L;
        expireTaskId = Bukkit.getScheduler()
                .runTaskTimer(this, () -> marketManager.deleteExpired(), ticks, ticks)
                .getTaskId();
    }

    public static Market getInstance() {
        return instance;
    }

    public MarketConfig getMarketConfig() {
        return marketConfig;
    }

    public MarketMenuConfig getMenuConfig() {
        return menuConfig;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public EconomyHook getEconomy() {
        return economy;
    }

    public MailBridge getMail() {
        return mail;
    }
}
