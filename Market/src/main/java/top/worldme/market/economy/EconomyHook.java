package top.worldme.market.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class EconomyHook {

    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyHook(JavaPlugin plugin) {
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
        return economy != null || setup();
    }

    public Economy economy() {
        return economy;
    }

    public double balance(UUID uuid) {
        if (!isReady()) {
            return 0;
        }
        return economy.getBalance(Bukkit.getOfflinePlayer(uuid));
    }

    public boolean has(UUID uuid, double amount) {
        return isReady() && economy.has(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!isReady()) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), amount);
        return response != null && response.transactionSuccess();
    }

    public boolean deposit(UUID uuid, double amount) {
        if (!isReady()) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
        return response != null && response.transactionSuccess();
    }

    public String format(double amount) {
        if (isReady()) {
            try {
                return economy.format(amount);
            } catch (Exception ignored) {
            }
        }
        return String.format("%.2f", amount);
    }

    @SuppressWarnings("unused")
    public OfflinePlayer offline(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }
}
