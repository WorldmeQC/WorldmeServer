package top.worldme.market.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.worldme.Market;
import top.worldme.market.gui.MainMenu;
import top.worldme.market.manager.MarketManager.MarketResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MarketCommand implements CommandExecutor, TabCompleter {

    private final Market plugin;

    public MarketCommand(Market plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<yellow>用法: /" + label + " sell <价格> | open <玩家> | reload"));
                return true;
            }
            if (!hasUse(sender)) {
                send(sender, "no-permission", null);
                return true;
            }
            open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>上架需要由玩家执行。"));
                    return true;
                }
                if (!hasUse(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>用法: /" + label + " sell <价格>"));
                    return true;
                }
                double price;
                try {
                    price = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    send(sender, "price-invalid", null);
                    return true;
                }
                sellHeld(player, price);
                return true;
            }
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
                open(target);
                return true;
            }
            case "reload" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                plugin.getMarketConfig().load();
                plugin.getMenuConfig().load();
                send(sender, "reloaded", null);
                return true;
            }
            default -> {
                if (sender instanceof Player player && hasUse(sender)) {
                    open(player);
                } else {
                    send(sender, "no-permission", null);
                }
                return true;
            }
        }
    }

    private void sellHeld(Player player, double price) {
        if (price < plugin.getMarketConfig().minPrice() || price > plugin.getMarketConfig().maxPrice()) {
            send(player, "price-invalid", null);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            send(player, "need-item", null);
            return;
        }

        MarketResult result = plugin.getMarketManager().create(player, item, price);
        switch (result) {
            case OK -> {
                player.getInventory().setItemInMainHand(null);
                player.updateInventory();
                double fee = plugin.getMarketConfig().listingFee(price);
                send(player, "listed", Map.of(
                        "price", plugin.getEconomy().format(price),
                        "fee", plugin.getEconomy().format(fee)
                ));
            }
            case NO_MONEY -> send(player, "no-money-fee", Map.of(
                    "fee", plugin.getEconomy().format(plugin.getMarketConfig().listingFee(price))));
            case BLACKLISTED -> send(player, "blacklisted", null);
            case PRICE_INVALID -> send(player, "price-invalid", null);
            case TOO_MANY -> send(player, "too-many", Map.of(
                    "max", String.valueOf(plugin.getMarketConfig().maxListingsPerPlayer())));
            case ECONOMY_ERROR -> send(player, "economy-error", null);
            default -> send(player, "error", null);
        }
    }

    private void open(Player player) {
        MainMenu menu = new MainMenu(plugin, player);
        player.openInventory(menu.getInventory());
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("worldme.market.admin") || !(sender instanceof Player);
    }

    private boolean hasUse(CommandSender sender) {
        return sender.hasPermission("worldme.market.use") || !(sender instanceof Player);
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String text = plugin.getMarketConfig().getMessage(key, placeholders);
        if (text == null || text.isEmpty()) {
            return;
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getMarketConfig().getMessage("prefix") + text));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (hasUse(sender)) {
                subs.add("sell");
            }
            if (hasAdmin(sender)) {
                subs.addAll(List.of("open", "reload"));
            }
            return subs;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("open".equals(sub) && hasAdmin(sender)) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
            if ("sell".equals(sub) && hasUse(sender)) {
                return List.of("<价格>");
            }
        }
        return Collections.emptyList();
    }
}
