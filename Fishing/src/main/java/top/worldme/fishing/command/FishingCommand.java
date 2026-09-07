package top.worldme.fishing.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.fishing.config.FishingConfig;
import top.worldme.fishing.config.MenuConfig;
import top.worldme.fishing.gui.QuestGui;
import top.worldme.fishing.quest.QuestManager;

import java.util.Collections;
import java.util.List;

public class FishingCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final QuestManager questManager;
    private final MenuConfig menuConfig;
    private final FishingConfig config;

    public FishingCommand(JavaPlugin plugin, QuestManager questManager, MenuConfig menuConfig, FishingConfig config) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.menuConfig = menuConfig;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                send(player, "no-quest-from-command");
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>用法: /wmfishing open <玩家> | /wmfishing reload"));
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "open" -> {
                if (!hasAdminPermission(sender)) {
                    if (sender instanceof Player player) {
                        send(player, "no-permission");
                    } else {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>你没有权限执行此命令。"));
                    }
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>用法: /wmfishing open <玩家>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            config.getMessage("player-not-found", java.util.Map.of("player", args[1]))
                    ));
                    return true;
                }
                openMenu(target);
                return true;
            }
            case "reload" -> {
                if (!hasAdminPermission(sender)) {
                    if (sender instanceof Player player) {
                        send(player, "no-permission");
                    } else {
                        sender.sendMessage("你没有权限执行此命令。");
                    }
                    return true;
                }
                config.load();
                menuConfig.load();
                String text = config.getMessage("config-reloaded");
                if (!text.isEmpty()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>配置已重载。"));
                }
                return true;
            }
            default -> {
                if (sender instanceof Player player) {
                    send(player, "no-quest-from-command");
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>未知子命令。"));
                }
                return true;
            }
        }
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("worldme.fishing.admin") || !(sender instanceof Player);
    }

    public void openMenu(Player player) {
        QuestGui gui = new QuestGui(questManager, menuConfig, player);
        player.openInventory(gui.getInventory());
    }

    private void send(Player player, String key) {
        String text = config.getMessage(key);
        if (text == null || text.isEmpty()) {
            return;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!hasAdminPermission(sender)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return java.util.Arrays.asList("open", "reload");
        }
        if (args.length == 2 && "open".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return Collections.emptyList();
    }
}
