package top.worldme.ownership.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.ownership.binding.BindManager;
import top.worldme.ownership.config.BindMenuConfig;
import top.worldme.ownership.config.OwnerConfig;
import top.worldme.ownership.economy.VaultHook;
import top.worldme.ownership.gui.BindGui;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BindCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final OwnerConfig config;
    private final BindMenuConfig menuConfig;
    private final BindManager bindManager;
    private final VaultHook vaultHook;

    public BindCommand(JavaPlugin plugin, OwnerConfig config, BindMenuConfig menuConfig, BindManager bindManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.config = config;
        this.menuConfig = menuConfig;
        this.bindManager = bindManager;
        this.vaultHook = vaultHook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                send(sender, "player-only", null);
                return true;
            }
            if (!hasUse(sender)) {
                send(sender, "no-permission", null);
                return true;
            }
            openMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>用法: /" + label + " open <玩家>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    send(sender, "player-not-found", Map.of("player", args[1]));
                    return true;
                }
                openMenu(target);
                return true;
            }
            case "unbind" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "player-only", null);
                    return true;
                }
                if (!hasUse(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                unbind(player);
                return true;
            }
            case "reload" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                config.load();
                menuConfig.load();
                send(sender, "reloaded", null);
                return true;
            }
            default -> {
                if (sender instanceof Player player && hasUse(sender)) {
                    openMenu(player);
                } else if (sender instanceof Player) {
                    send(sender, "no-permission", null);
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>未知子命令。"));
                }
                return true;
            }
        }
    }

    public void openMenu(Player player) {
        BindGui gui = new BindGui(plugin, menuConfig, config, bindManager, vaultHook, player);
        player.openInventory(gui.getInventory());
    }

    private void unbind(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir() || !bindManager.isBound(item)) {
            send(player, "unbind-not-owned", null);
            return;
        }
        if (!bindManager.isOwnedBy(item, player.getUniqueId()) && !player.hasPermission("worldme.ownership.admin")) {
            send(player, "unbind-not-owner", null);
            return;
        }
        double price = config.unbindPrice();
        if (price > 0) {
            if (!vaultHook.isReady()) {
                send(player, "no-economy", null);
                return;
            }
            var economy = vaultHook.economy();
            if (!economy.has(player, price)) {
                send(player, "unbind-not-enough-money", Map.of("price", vaultHook.format(price)));
                return;
            }
            var response = economy.withdrawPlayer(player, price);
            if (!response.transactionSuccess()) {
                send(player, "no-economy", null);
                return;
            }
        }
        bindManager.unbind(item);
        send(player, "unbind-success", null);
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("worldme.ownership.admin") || !(sender instanceof Player);
    }

    private boolean hasUse(CommandSender sender) {
        return sender.hasPermission("worldme.ownership.use") || !(sender instanceof Player);
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String text = config.getMessage(key, placeholders);
        if (text == null || text.isEmpty()) {
            return;
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            if (hasAdmin(sender)) {
                return java.util.Arrays.asList("open", "unbind", "reload");
            }
            return Collections.singletonList("unbind");
        }
        if (args.length == 2 && "open".equalsIgnoreCase(args[0]) && hasAdmin(sender)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return Collections.emptyList();
    }
}
