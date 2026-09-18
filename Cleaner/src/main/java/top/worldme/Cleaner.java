package top.worldme;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.cleaner.command.CleanerCommand;
import top.worldme.cleaner.config.CleanerConfig;
import top.worldme.cleaner.config.TrashMenuConfig;
import top.worldme.cleaner.gui.TrashCanGui;
import top.worldme.cleaner.manager.ClearManager;

public class Cleaner extends JavaPlugin {
    private static Cleaner instance;
    private CleanerConfig cleanerConfig;
    private TrashMenuConfig menuConfig;
    private ClearManager clearManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("menu.yml", false);

        this.cleanerConfig = new CleanerConfig(this);
        this.menuConfig = new TrashMenuConfig(this);
        this.clearManager = new ClearManager(this, cleanerConfig);
        this.clearManager.start();

        Bukkit.getPluginManager().registerEvents(new TrashCanGui.GuiListener(this, menuConfig, clearManager), this);

        CleanerCommand command = new CleanerCommand(this, cleanerConfig, menuConfig, clearManager);
        getCommand("wmcleaner").setExecutor(command);
        getCommand("wmcleaner").setTabCompleter(command);

        getLogger().info("Worldme-Cleaner 已加载。");
    }

    @Override
    public void onDisable() {
        if (clearManager != null) {
            clearManager.cancel();
        }
        getLogger().info("Worldme-Cleaner 已卸载。");
    }

    public static Cleaner getInstance() {
        return instance;
    }

    public CleanerConfig getCleanerConfig() {
        return cleanerConfig;
    }

    public TrashMenuConfig getMenuConfig() {
        return menuConfig;
    }

    public ClearManager getClearManager() {
        return clearManager;
    }
}