package top.worldme.guild.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.worldme.Guild;
import top.worldme.guild.config.GuildConfig;
import top.worldme.guild.data.GuildFundRequest;
import top.worldme.guild.data.GuildMember;
import top.worldme.guild.data.GuildRank;
import top.worldme.guild.gui.GuildMenu;
import top.worldme.guild.manager.GuildManager;
import top.worldme.guild.manager.GuildManager.GuildResult;
import top.worldme.territory.data.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class GuildCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "info", "join", "cancel", "accept", "reject", "invite", "kick", "rank",
            "leave", "disband", "deposit", "withdraw", "request", "fundapprove", "fundreject",
            "fee", "openjoin", "upgrade", "tp", "open", "reload");

    private final Guild plugin;
    private final GuildConfig config;
    private final GuildManager manager;

    public GuildCommand(Guild plugin, GuildConfig config, GuildManager manager) {
        this.plugin = plugin;
        this.config = config;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (!hasUse(player)) {
                    send(player, "no-permission", null);
                    return true;
                }
                open(player);
            } else {
                send(sender, "invalid-usage", Map.of("usage", "/" + label + " info"));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> withPlayer(sender, player -> handleCreate(player, label, args));
            case "info" -> withPlayer(sender, player -> handleInfo(player));
            case "join" -> withPlayer(sender, player -> handleJoin(player, label, args));
            case "cancel" -> withPlayer(sender, player -> {
                manager.cancelJoinRequest(player);
                send(player, "join-request-cancelled", null);
            });
            case "accept" -> withPlayer(sender, player -> handleAccept(player, label, args));
            case "reject" -> withPlayer(sender, player -> handleReject(player, label, args));
            case "invite" -> withPlayer(sender, player -> handleInvite(player, label, args));
            case "kick" -> withPlayer(sender, player -> handleKick(player, label, args));
            case "rank" -> withPlayer(sender, player -> handleRank(player, label, args));
            case "leave" -> withPlayer(sender, player -> handleLeave(player));
            case "disband" -> withPlayer(sender, player -> handleDisband(player));
            case "deposit" -> withPlayer(sender, player -> handleDeposit(player, label, args));
            case "withdraw" -> withPlayer(sender, player -> handleWithdraw(player, label, args));
            case "request" -> withPlayer(sender, player -> handleRequest(player, label, args));
            case "fundapprove" -> withPlayer(sender, player -> handleFundApprove(player, label, args));
            case "fundreject" -> withPlayer(sender, player -> handleFundReject(player, label, args));
            case "fee" -> withPlayer(sender, player -> handleFee(player, label, args));
            case "openjoin" -> withPlayer(sender, player -> handleOpenJoin(player));
            case "upgrade" -> withPlayer(sender, player -> handleUpgrade(player));
            case "tp" -> withPlayer(sender, player -> handleTp(player));
            case "open" -> handleOpen(sender, label, args);
            case "reload" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                config.load();
                plugin.getMenuConfig().load();
                plugin.getStructureConfig().load();
                send(sender, "reloaded", null);
            }
            default -> send(sender, "invalid-usage", Map.of("usage", "/" + label + " info"));
        }
        return true;
    }

    private void open(Player player) {
        player.openInventory(new GuildMenu(plugin, player).getInventory());
    }

    private void handleCreate(Player player, String label, String[] args) {
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " create <名称>"));
            return;
        }
        GuildResult result = manager.create(player, args[1]);
        switch (result) {
            case OK -> send(player, "create-success", Map.of("name", args[1]));
            case IN_GUILD -> send(player, "create-in-guild", null);
            case NAME_TAKEN -> send(player, "create-name-taken", null);
            case NAME_INVALID -> send(player, "create-name-invalid", null);
            case NO_MONEY -> send(player, "create-no-money", Map.of("cost", manager.vault().format(config.createCost())));
            case REGION_UNAVAILABLE -> send(player, "region-unavailable", null);
            default -> send(player, "create-failed", null);
        }
    }

    private void handleInfo(Player player) {
        top.worldme.guild.data.Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        Region region = manager.regionOf(guild);
        String leader = name(guild.leader());
        send(player, "info-header", Map.of("name", guild.name(), "level", String.valueOf(guild.level())));
        send(player, "info-body", Map.of(
                "leader", leader,
                "members", String.valueOf(guild.memberCount()),
                "balance", manager.vault().format(guild.balance())));
        send(player, "info-region", Map.of("region", region == null ? "无" : region.name()));
    }

    private void handleJoin(Player player, String label, String[] args) {
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " join <公会>"));
            return;
        }
        top.worldme.guild.data.Guild guild = manager.guildByName(args[1]);
        if (guild == null) {
            send(player, "guild-not-found", Map.of("name", args[1]));
            return;
        }
        GuildResult result = guild.openJoin() ? manager.join(player, guild) : manager.requestJoin(player, guild);
        switch (result) {
            case OK -> send(player, guild.openJoin() ? "join-success" : "join-requested",
                    Map.of("name", guild.name(), "fee", manager.vault().format(guild.joinFee())));
            case OPEN_JOIN -> send(player, "join-open", null);
            case IN_GUILD -> send(player, "create-in-guild", null);
            case NO_MONEY -> send(player, "join-no-money", Map.of("fee", manager.vault().format(guild.joinFee())));
            case REJOIN_COOLDOWN -> send(player, "join-cooldown", null);
            case ALREADY_APPLIED -> send(player, "join-already", null);
            case MEMBER_LIMIT -> send(player, "member-limit", Map.of("max", String.valueOf(manager.memberCap(guild))));
            default -> send(player, "invalid-usage", Map.of("usage", "/" + label + " join <公会>"));
        }
    }

    private void handleAccept(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " accept <玩家>"));
            return;
        }
        UUID target = resolveTarget(args[1]);
        if (target == null) {
            send(player, "player-not-found", Map.of("player", args[1]));
            return;
        }
        GuildResult result = manager.approveJoin(player, guild, target);
        switch (result) {
            case OK -> send(player, "join-accepted-actor", Map.of("player", args[1]));
            case NO_PERMISSION -> send(player, "no-permission", null);
            case NOT_FOUND -> send(player, "player-not-found", Map.of("player", args[1]));
            case NO_MONEY -> send(player, "join-no-money", Map.of("fee", manager.vault().format(guild.joinFee())));
            case MEMBER_LIMIT -> send(player, "member-limit", Map.of("max", String.valueOf(manager.memberCap(guild))));
            default -> send(player, "invalid-usage", Map.of("usage", "/" + label + " accept <玩家>"));
        }
    }

    private void handleReject(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " reject <玩家>"));
            return;
        }
        UUID target = resolveTarget(args[1]);
        if (target == null) {
            send(player, "player-not-found", Map.of("player", args[1]));
            return;
        }
        manager.denyJoin(player, guild, target);
        send(player, "join-rejected-actor", Map.of("player", args[1]));
    }

    private void handleInvite(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " invite <玩家>"));
            return;
        }
        UUID target = resolveTarget(args[1]);
        if (target == null) {
            send(player, "player-not-found", Map.of("player", args[1]));
            return;
        }
        GuildResult result = manager.addMember(player, guild, target);
        switch (result) {
            case OK -> send(player, "member-added", Map.of("player", args[1]));
            case MEMBER_EXISTS -> send(player, "member-already", null);
            case MEMBER_LIMIT -> send(player, "member-limit", Map.of("max", String.valueOf(manager.memberCap(guild))));
            case NO_PERMISSION -> send(player, "no-permission", null);
            default -> send(player, "invalid-usage", Map.of("usage", "/" + label + " invite <玩家>"));
        }
    }

    private void handleKick(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " kick <玩家>"));
            return;
        }
        UUID target = resolveTarget(args[1]);
        if (target == null) {
            send(player, "player-not-found", Map.of("player", args[1]));
            return;
        }
        GuildResult result = manager.kick(player, guild, target);
        switch (result) {
            case OK -> send(player, "kick-success", Map.of("player", args[1]));
            case IS_LEADER -> send(player, "disband-owner-only", null);
            case NO_PERMISSION -> send(player, "no-permission", null);
            default -> send(player, "invalid-usage", Map.of("usage", "/" + label + " kick <玩家>"));
        }
    }

    private void handleRank(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        if (args.length < 3) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " rank <玩家> <officer|member>"));
            return;
        }
        UUID target = resolveTarget(args[1]);
        if (target == null) {
            send(player, "player-not-found", Map.of("player", args[1]));
            return;
        }
        String rankKey = args[2].toLowerCase();
        GuildRank rank = guild.rank(rankKey);
        if (rank == null || rankKey.equals(GuildManager.RANK_LEADER)) {
            send(player, "rank-invalid", Map.of("rank", args[2]));
            return;
        }
        GuildResult result = manager.setRank(player, guild, target, rankKey);
        switch (result) {
            case OK -> send(player, "rank-set", Map.of("player", args[1], "rank", rank.display()));
            case NO_PERMISSION -> send(player, "no-permission", null);
            case IS_LEADER -> send(player, "disband-owner-only", null);
            default -> send(player, "invalid-usage", Map.of("usage", "/" + label + " rank <玩家> <officer|member>"));
        }
    }

    private void handleLeave(Player player) {
        GuildResult result = manager.leave(player);
        switch (result) {
            case OK -> send(player, "leave-success", null);
            case IS_LEADER -> send(player, "leave-owner", null);
            case TOO_SOON -> send(player, "leave-too-soon", Map.of("days", String.valueOf(config.noLeaveDays())));
            default -> send(player, "no-guild", null);
        }
    }

    private void handleDisband(Player player) {
        GuildResult result = manager.disband(player);
        switch (result) {
            case OK -> send(player, "disband-success", null);
            case NOT_LEADER -> send(player, "disband-owner-only", null);
            default -> send(player, "no-guild", null);
        }
    }

    private void handleDeposit(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        Double amount = parseAmount(args, 1);
        if (amount == null || amount <= 0) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " deposit <金额>"));
            return;
        }
        if (manager.deposit(player.getUniqueId(), amount)) {
            send(player, "deposit-success", Map.of("amount", manager.vault().format(amount)));
        } else {
            send(player, "deposit-fail", null);
        }
    }

    private void handleWithdraw(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        Double amount = parseAmount(args, 1);
        if (amount == null || amount <= 0) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " withdraw <金额>"));
            return;
        }
        if (manager.withdraw(player, amount)) {
            send(player, "withdraw-success", Map.of("amount", manager.vault().format(amount)));
        } else {
            send(player, "withdraw-fail", null);
        }
    }

    private void handleRequest(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        Double amount = parseAmount(args, 1);
        if (amount == null || amount <= 0) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " request <金额>"));
            return;
        }
        if (manager.requestFunds(player, guild, amount) == GuildResult.OK) {
            send(player, "fund-request-sent", null);
        } else {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " request <金额>"));
        }
    }

    private void handleFundApprove(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        Integer id = parseId(args, 1);
        if (id == null) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " fundapprove <编号>"));
            return;
        }
        GuildResult result = manager.approveFund(player, guild, id);
        switch (result) {
            case OK -> send(player, "fund-approved", Map.of("id", String.valueOf(id)));
            case NO_MONEY -> send(player, "withdraw-fail", null);
            case NO_PERMISSION -> send(player, "no-permission", null);
            default -> send(player, "invalid-usage", Map.of("usage", "/" + label + " fundapprove <编号>"));
        }
    }

    private void handleFundReject(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        Integer id = parseId(args, 1);
        if (id == null) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " fundreject <编号>"));
            return;
        }
        manager.denyFund(player, guild, id);
        send(player, "fund-rejected", Map.of("id", String.valueOf(id)));
    }

    private void handleFee(Player player, String label, String[] args) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        Double amount = parseAmount(args, 1);
        if (amount == null || amount < 0) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " fee <金额>"));
            return;
        }
        GuildResult result = manager.setJoinFee(player, guild, amount);
        if (result == GuildResult.OK) {
            send(player, "join-fee-set", Map.of("fee", manager.vault().format(guild.joinFee())));
        } else if (result == GuildResult.FEE_TOO_LOW) {
            send(player, "amount-invalid", null);
        } else {
            send(player, "no-permission", null);
        }
    }

    private void handleOpenJoin(Player player) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        boolean open = !guild.openJoin();
        GuildResult result = manager.setOpenJoin(player, guild, open);
        if (result == GuildResult.OK) {
            send(player, open ? "open-join-on" : "open-join-off", null);
        } else {
            send(player, "no-permission", null);
        }
    }

    private void handleUpgrade(Player player) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        GuildResult result = manager.upgrade(player);
        switch (result) {
            case OK -> send(player, "upgrade-success", Map.of("level", String.valueOf(guild.level())));
            case MAX_LEVEL -> send(player, "upgrade-max", null);
            case NO_MONEY -> send(player, "upgrade-no-money", Map.of(
                    "cost", manager.vault().format(config.upgradeCostFor(guild.level()))));
            case NOT_LEADER -> send(player, "disband-owner-only", null);
            default -> send(player, "upgrade-failed", null);
        }
    }

    private void handleTp(Player player) {
        Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            send(player, "no-guild", null);
            return;
        }
        Region region = manager.regionOf(guild);
        if (region == null || !region.hasWarp()) {
            send(player, "tp-no-region", null);
            return;
        }
        Location warp = region.getWarp();
        if (warp == null) {
            send(player, "tp-no-region", null);
            return;
        }
        player.teleport(warp);
        send(player, "tp-success", null);
    }

    private void handleOpen(CommandSender sender, String label, String[] args) {
        if (!hasAdmin(sender)) {
            send(sender, "no-permission", null);
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                send(sender, "player-not-found", Map.of("player", args[1]));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            send(sender, "invalid-usage", Map.of("usage", "/" + label + " open <玩家>"));
            return;
        }
        open(target);
        send(sender, "open-success", Map.of("player", target.getName()));
    }

    private void withPlayer(CommandSender sender, Consumer<Player> action) {
        if (sender instanceof Player player) {
            action.accept(player);
        } else {
            send(sender, "player-only", null);
        }
    }

    private UUID resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }

    private Double parseAmount(String[] args, int index) {
        if (args.length <= index) {
            return null;
        }
        try {
            return Double.parseDouble(args[index]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseId(String[] args, int index) {
        if (args.length <= index) {
            return null;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String name(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "未知" : name;
    }

    private boolean hasUse(CommandSender sender) {
        return sender.hasPermission("worldme.guild.use") || !(sender instanceof Player);
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("worldme.guild.admin") || !(sender instanceof Player);
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
            List<String> result = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    result.add(sub);
                }
            }
            return result;
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            if (sub.equals("join")) {
                List<String> names = new ArrayList<>();
                for (top.worldme.guild.data.Guild guild : manager.all()) {
                    names.add(guild.name());
                }
                return names;
            }
            if (sub.equals("accept") || sub.equals("reject") || sub.equals("invite")
                    || sub.equals("kick") || sub.equals("rank") || sub.equals("open")) {
                return onlineNames();
            }
        }
        if (args.length == 3 && sub.equals("rank")) {
            return List.of("officer", "member");
        }
        return Collections.emptyList();
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
