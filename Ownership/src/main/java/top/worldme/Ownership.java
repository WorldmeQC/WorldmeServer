package top.worldme;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.ownership.binding.BindManager;
import top.worldme.ownership.command.BindCommand;
import top.worldme.ownership.config.BindMenuConfig;
import top.worldme.ownership.config.OwnerConfig;
import top.worldme.ownership.economy.VaultHook;
import top.worldme.ownership.gui.BindGui;
import top.worldme.ownership.listener.BindListener;
import top.worldme.ownership.util.OwnerKeys;

public class Ownership extends JavaPlugin {

    private static Ownership instance;
    private OwnerConfig ownerConfig;
    private BindMenuConfig menuConfig;
    private BindManager bindManager;
    private VaultHook vaultHook;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("menu.yml", false);

        this.ownerConfig = new OwnerConfig(this);
        this.menuConfig = new BindMenuConfig(this);
        OwnerKeys keys = new OwnerKeys(this);
        this.bindManager = new BindManager(ownerConfig, keys);
        this.vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            getLogger().warning("未检测到 Vault 经济系统，认主功能将不可用。");
        }

        Bukkit.getPluginManager().registerEvents(new BindListener(ownerConfig, bindManager), this);
        Bukkit.getPluginManager().registerEvents(new BindGui.GuiListener(menuConfig, ownerConfig, bindManager, vaultHook), this);

        BindCommand command = new BindCommand(this, ownerConfig, menuConfig, bindManager, vaultHook);
        getCommand("wmbind").setExecutor(command);
        getCommand("wmbind").setTabCompleter(command);

        getLogger().info("Worldme-Ownership 已加载。");
    }

    public static Ownership getInstance() {
        return instance;
    }
}
