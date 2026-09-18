package top.worldme.mail.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.mail.config.MailConfig;
import top.worldme.mail.config.MailMenuConfig;
import top.worldme.mail.gui.ComposeGui;
import top.worldme.mail.gui.MailGui;
import top.worldme.mail.manager.MailManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MailCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MailConfig config;
    private final MailMenuConfig menuConfig;
    private final MailManager mailManager;

    public MailCommand(JavaPlugin plugin, MailConfig config, MailMenuConfig menuConfig, MailManager mailManager) {
        this.plugin = plugin;
        this.config = config;
        this.menuConfig = menuConfig;
        this.mailManager = mailManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<yellow>用法: /" + label + " open <玩家> | send <玩家> | cmd <玩家> <主题> <指令...> | clear <玩家> | reload"));
                return true;
            }
            if (!hasUse(sender)) {
                send(sender, "no-permission", null);
                return true;
            }
            openMailbox(player);
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
                openMailbox(target);
                return true;
            }
            case "send" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                if (!(sender instanceof Player composer)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>写信功能需要由玩家执行。"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>用法: /" + label + " send <玩家>"));
                    return true;
                }
                OfflinePlayer target = resolveTarget(args[1]);
                if (target == null) {
                    send(sender, "player-not-found", Map.of("player", args[1]));
                    return true;
                }
                ComposeGui gui = new ComposeGui(plugin, menuConfig, config, mailManager, composer, target.getUniqueId(), target.getName());
                composer.openInventory(gui.getInventory());
                return true;
            }
            case "cmd" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<yellow>用法: /" + label + " cmd <玩家> <主题> <指令...>（多条指令用 | 分隔）"));
                    return true;
                }
                OfflinePlayer target = resolveTarget(args[1]);
                if (target == null) {
                    send(sender, "player-not-found", Map.of("player", args[1]));
                    return true;
                }
                String subject = args[2];
                String joined = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                List<String> commands = new ArrayList<>();
                for (String part : joined.split("\\s*\\|\\s*")) {
                    if (!part.isBlank()) {
                        commands.add(part);
                    }
                }
                mailManager.sendCommands(target.getUniqueId(), subject, commands);
                send(sender, "mail-sent", Map.of("player", target.getName()));
                return true;
            }
            case "clear" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>用法: /" + label + " clear <玩家>"));
                    return true;
                }
                OfflinePlayer target = resolveTarget(args[1]);
                if (target == null) {
                    send(sender, "player-not-found", Map.of("player", args[1]));
                    return true;
                }
                int count = mailManager.countMails(target.getUniqueId());
                mailManager.clear(target.getUniqueId());
                send(sender, "mail-cleared", Map.of("player", target.getName(), "count", String.valueOf(count)));
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
                    openMailbox(player);
                } else if (sender instanceof Player) {
                    send(sender, "no-permission", null);
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>未知子命令。"));
                }
                return true;
            }
        }
    }

    private void openMailbox(Player player) {
        MailGui gui = new MailGui(plugin, menuConfig, config, mailManager, player);
        player.openInventory(gui.getInventory());
    }

    private OfflinePlayer resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("worldme.mail.admin") || !(sender instanceof Player);
    }

    private boolean hasUse(CommandSender sender) {
        return sender.hasPermission("worldme.mail.use") || !(sender instanceof Player);
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
                return Arrays.asList("open", "send", "cmd", "clear", "reload");
            }
            return Collections.emptyList();
        }
        if (args.length == 2 && hasAdmin(sender)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return Collections.emptyList();
    }
}