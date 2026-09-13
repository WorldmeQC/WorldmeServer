package top.worldme.ownership.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultHook {

    private final JavaPlugin plugin;
    private Economy economy;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isReady() {
        if (economy != null) {
            return true;
        }
        return setup();
    }

    public Economy economy() {
        return economy;
    }

    public String format(double amount) {
        if (economy != null) {
            try {
                return economy.format(amount);
            } catch (Exception ignored) {
            }
        }
        return String.format("%.2f", amount);
    }
}
