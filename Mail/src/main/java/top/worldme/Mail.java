package top.worldme;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.mail.api.MailApi;
import top.worldme.mail.command.MailCommand;
import top.worldme.mail.config.MailConfig;
import top.worldme.mail.config.MailMenuConfig;
import top.worldme.mail.data.MailDatabase;
import top.worldme.mail.gui.ComposeGui;
import top.worldme.mail.gui.MailGui;
import top.worldme.mail.listener.MailListener;
import top.worldme.mail.manager.MailManager;

public class Mail extends JavaPlugin {

    private static Mail instance;
    private MailConfig mailConfig;
    private MailMenuConfig menuConfig;
    private MailDatabase database;
    private MailManager mailManager;
    private int cleanupTaskId = -1;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("menu.yml", false);

        this.mailConfig = new MailConfig(this);
        this.menuConfig = new MailMenuConfig(this);
        this.database = new MailDatabase(this);
        if (!database.open()) {
            getLogger().severe("数据库初始化失败，插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.mailManager = new MailManager(this, mailConfig, database);
        mailManager.deleteExpired();
        startCleanupTask();

        Bukkit.getPluginManager().registerEvents(new MailListener(this, mailConfig, mailManager), this);
        Bukkit.getPluginManager().registerEvents(new MailGui.GuiListener(this, menuConfig, mailConfig, mailManager), this);
        Bukkit.getPluginManager().registerEvents(new ComposeGui.GuiListener(this, menuConfig, mailConfig, mailManager), this);

        MailCommand command = new MailCommand(this, mailConfig, menuConfig, mailManager);
        getCommand("wmmail").setExecutor(command);
        getCommand("wmmail").setTabCompleter(command);

        getLogger().info("Worldme-Mail 已加载。");
    }

    @Override
    public void onDisable() {
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("Worldme-Mail 已卸载。");
    }

    private void startCleanupTask() {
        long ticks = mailConfig.cleanupInterval() * 20L;
        cleanupTaskId = Bukkit.getScheduler()
                .runTaskTimer(this, () -> mailManager.deleteExpired(), ticks, ticks)
                .getTaskId();
    }

    public static Mail getInstance() {
        return instance;
    }

    public MailManager getMailManager() {
        return mailManager;
    }

    /**
     * 供其他模块调用邮件功能。
     */
    public MailApi getApi() {
        return mailManager;
    }
}