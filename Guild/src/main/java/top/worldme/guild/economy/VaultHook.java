package top.worldme.guild.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

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
        return economy != null || setup();
    }

    public Economy economy() {
        return economy;
    }

    public boolean has(UUID uuid, double amount) {
        return isReady() && economy.has(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!isReady() || amount <= 0) {
            return amount <= 0;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    public boolean deposit(UUID uuid, double amount) {
        if (!isReady() || amount <= 0) {
            return amount <= 0;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        EconomyResponse response = economy.depositPlayer(player, amount);
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
}
