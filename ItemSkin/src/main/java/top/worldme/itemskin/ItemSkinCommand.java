package top.worldme.itemskin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.worldme.itemskin.service.SkinService;

import java.util.Collections;
import java.util.List;

public class ItemSkinCommand implements CommandExecutor, TabCompleter {

    private final ItemSkinConfig config;
    private final SkinService skinService;

    public ItemSkinCommand(ItemSkinConfig config, SkinService skinService) {
        this.config = config;
        this.skinService = skinService;
    }

    private void reloadConfig() {
        skinService.clearSamples();
        config.reload();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unload" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c该子命令只能由玩家执行。");
                    return true;
                }
                handleUnload(player);
            }
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender, label);
        }

        return true;
    }

    private void handleUnload(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType().isAir()) {
            player.sendMessage("§c请手持要解除外观的武器。");
            return;
        }

        if (skinService.unloadSkin(player, mainHand)) {
            player.sendMessage("§a外观已解除，皮肤物品已返还。");
        } else {
            player.sendMessage("§c该物品未绑定外观。");
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            reloadConfig();
            sender.sendMessage("§aItemSkin 配置已重载。");
        } catch (Exception e) {
            sender.sendMessage("§c重载配置时发生错误，请查看后台日志。");
            throw e;
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§6===== ItemSkin 用法 =====");
        sender.sendMessage("§e/" + label + " unload §7- 解除主手物品的外观");
        sender.sendMessage("§e/" + label + " reload §7- 重载配置文件");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("unload", "reload");
        }
        return Collections.emptyList();
    }
}
