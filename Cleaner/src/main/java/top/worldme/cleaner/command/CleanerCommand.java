package top.worldme.cleaner.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.cleaner.config.CleanerConfig;
import top.worldme.cleaner.config.TrashMenuConfig;
import top.worldme.cleaner.gui.TrashCanGui;
import top.worldme.cleaner.manager.ClearManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CleanerCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final CleanerConfig config;
    private final TrashMenuConfig menuConfig;
    private final ClearManager clearManager;

    public CleanerCommand(JavaPlugin plugin, CleanerConfig config, TrashMenuConfig menuConfig, ClearManager clearManager) {
        this.plugin = plugin;
        this.config = config;
        this.menuConfig = menuConfig;
        this.clearManager = clearManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>用法: /" + label + " open <玩家> | cleartrash | reload"));
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
            case "cleartrash" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                int count = clearManager.trashSize();
                clearManager.clearTrash();
                send(sender, "trash-cleared", Map.of("count", String.valueOf(count)));
                return true;
            }
            case "reload" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                config.load();
                menuConfig.load();
                clearManager.start();
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
        TrashCanGui gui = new TrashCanGui(plugin, menuConfig, config, clearManager, player);
        player.openInventory(gui.getInventory());
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("worldme.cleaner.admin") || !(sender instanceof Player);
    }

    private boolean hasUse(CommandSender sender) {
        return sender.hasPermission("worldme.cleaner.use") || !(sender instanceof Player);
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
                return Arrays.asList("open", "cleartrash", "reload");
            }
            return Collections.emptyList();
        }
        if (args.length == 2 && "open".equalsIgnoreCase(args[0]) && hasAdmin(sender)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return Collections.emptyList();
    }
}