package top.worldme.territory.command;

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
import top.worldme.Territory;
import top.worldme.territory.config.TerritoryConfig;
import top.worldme.territory.data.Direction;
import top.worldme.territory.data.OwnerType;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.SettingFlag;
import top.worldme.territory.data.SubRegion;
import top.worldme.territory.gui.RegionListMenu;
import top.worldme.territory.manager.RegionManager;
import top.worldme.territory.manager.RegionManager.RegionResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TerritoryCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "claim", "expand", "tp", "sethome", "unclaim", "name", "enter", "leave",
            "member", "perm", "setting", "subregion", "open", "reload");
    private static final List<String> DIRECTIONS = List.of("north", "south", "east", "west", "up", "down");

    private final Territory plugin;
    private final TerritoryConfig config;
    private final RegionManager manager;

    public TerritoryCommand(Territory plugin, TerritoryConfig config, RegionManager manager) {
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
                openMenu(player);
            } else {
                send(sender, "invalid-usage", Map.of("usage", "/" + label + " claim"));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "claim" -> withPlayer(sender, player -> handleClaim(player));
            case "expand" -> withPlayer(sender, player -> handleExpand(player, label, args));
            case "tp" -> withPlayer(sender, player -> handleTp(player, label, args));
            case "sethome" -> withPlayer(sender, player -> handleSetHome(player, label, args));
            case "unclaim" -> withPlayer(sender, player -> handleUnclaim(player, label, args));
            case "name" -> withPlayer(sender, player -> handleName(player, label, args));
            case "enter" -> withPlayer(sender, player -> handleMessage(player, label, args, true));
            case "leave" -> withPlayer(sender, player -> handleMessage(player, label, args, false));
            case "member" -> withPlayer(sender, player -> handleMember(player, label, args));
            case "perm" -> withPlayer(sender, player -> handlePerm(player, label, args));
            case "setting" -> withPlayer(sender, player -> handleSetting(player, label, args));
            case "subregion" -> handleSubRegion(sender, label, args);
            case "open" -> handleOpen(sender, label, args);
            case "reload" -> {
                if (!hasAdmin(sender)) {
                    send(sender, "no-permission", null);
                    return true;
                }
                config.load();
                send(sender, "reloaded", null);
            }
            default -> send(sender, "invalid-usage", Map.of("usage", "/" + label + " claim"));
        }
        return true;
    }

    public void openMenu(Player player) {
        player.openInventory(new RegionListMenu(plugin, player).getInventory());
    }

    private void handleClaim(Player player) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        RegionResult result = manager.create(player);
        switch (result) {
            case OK -> send(player, "claim-success", Map.of(
                    "cost", manager.vault().format(config.costForUnits(1)),
                    "count", String.valueOf(manager.countByOwner(OwnerType.PLAYER, player.getUniqueId())),
                    "max", String.valueOf(manager.maxClaims(player))));
            case AT_CAP -> send(player, "claim-at-cap", Map.of("max", String.valueOf(manager.maxClaims(player))));
            case OVERLAP -> send(player, "claim-overlap", null);
            case WORLD_DENIED -> send(player, "claim-world-denied", null);
            case WORLD_BOUND -> send(player, "expand-world-bound", null);
            case NO_MONEY -> send(player, "claim-no-money", Map.of("cost", manager.vault().format(config.costForUnits(1))));
            case ECONOMY -> send(player, "claim-economy", null);
            default -> send(player, "invalid-usage", Map.of("usage", "/wml claim"));
        }
    }

    private void handleExpand(Player player, String label, String[] args) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        if (args.length < 3) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " expand <方向> <名称> [单位]"));
            return;
        }
        Direction direction = Direction.byName(args[1]);
        if (direction == null) {
            send(player, "invalid-direction", Map.of("direction", args[1]));
            return;
        }
        int units = 1;
        if (args.length >= 4) {
            try {
                units = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                send(player, "invalid-amount", null);
                return;
            }
        }
        if (units <= 0) {
            send(player, "invalid-amount", null);
            return;
        }
        Region region = manager.findOwnedByName(player, args[2]);
        if (region == null) {
            region = manager.getRegionAt(player.getLocation());
            if (region == null || !region.ownerId().equals(player.getUniqueId())) {
                send(player, "region-not-found", Map.of("name", args[2]));
                return;
            }
        }
        double cost = manager.expandCost(region, direction, units);
        RegionResult result = manager.expand(player, region, direction, units);
        switch (result) {
            case OK -> send(player, "expand-success", Map.of(
                    "direction", args[1],
                    "units", String.valueOf(units),
                    "cost", manager.vault().format(cost)));
            case LIMIT -> send(player, "expand-limit", null);
            case WORLD_BOUND -> send(player, "expand-world-bound", null);
            case OVERLAP -> send(player, "expand-overlap", null);
            case NO_MONEY -> send(player, "expand-no-money", Map.of("cost", manager.vault().format(cost)));
            case ECONOMY -> send(player, "claim-economy", null);
            case NOT_OWNER -> send(player, "expand-not-owner", null);
            default -> send(player, "invalid-usage", Map.of("usage", "/" + label + " expand <方向> <名称> [单位]"));
        }
    }

    private void handleTp(Player player, String label, String[] args) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " tp <名称>"));
            return;
        }
        Region region = manager.findOwnedByName(player, args[1]);
        if (region == null) {
            send(player, "region-not-found", Map.of("name", args[1]));
            return;
        }
        if (!region.hasWarp()) {
            send(player, "home-not-set", null);
            return;
        }
        Location warp = region.getWarp();
        if (warp == null) {
            send(player, "region-not-found", Map.of("name", args[1]));
            return;
        }
        player.teleport(warp);
        send(player, "tp-success", Map.of("name", region.name()));
    }

    private void handleSetHome(Player player, String label, String[] args) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        Region region;
        if (args.length >= 2) {
            region = manager.findOwnedByName(player, args[1]);
        } else {
            Region at = manager.getRegionAt(player.getLocation());
            region = (at != null && at.ownerId().equals(player.getUniqueId())) ? at : null;
        }
        if (region == null) {
            send(player, "region-not-found", Map.of("name", args.length >= 2 ? args[1] : ""));
            return;
        }
        if (!region.contains(player.getLocation())) {
            send(player, "sethome-outside", null);
            return;
        }
        manager.setHome(region, player.getLocation());
        send(player, "sethome-success", null);
    }

    private void handleUnclaim(Player player, String label, String[] args) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " unclaim <名称>"));
            return;
        }
        Region region = manager.findOwnedByName(player, args[1]);
        if (region == null) {
            send(player, "region-not-found", Map.of("name", args[1]));
            return;
        }
        RegionResult result = manager.unclaim(player, region);
        if (result == RegionResult.OK) {
            send(player, "unclaim-success", Map.of("name", region.name()));
        } else if (result == RegionResult.NOT_OWNER) {
            send(player, "region-not-owner", null);
        } else {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " unclaim <名称>"));
        }
    }

    private void handleName(Player player, String label, String[] args) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        if (args.length < 3) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " name <名称> <新名称>"));
            return;
        }
        Region region = manager.findOwnedByName(player, args[1]);
        if (region == null) {
            send(player, "region-not-found", Map.of("name", args[1]));
            return;
        }
        manager.rename(region, args[2]);
        send(player, "name-success", Map.of("name", region.name()));
    }

    private void handleMessage(Player player, String label, String[] args, boolean enter) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        String usage = "/" + label + (enter ? " enter" : " leave") + " <名称> <消息>";
        if (args.length < 2) {
            send(player, "invalid-usage", Map.of("usage", usage));
            return;
        }
        Region region = manager.findOwnedByName(player, args[1]);
        if (region == null) {
            send(player, "region-not-found", Map.of("name", args[1]));
            return;
        }
        String message = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        if (enter) {
            manager.setEnterMessage(region, message);
            send(player, "enter-msg-set", null);
        } else {
            manager.setLeaveMessage(region, message);
            send(player, "leave-msg-set", null);
        }
    }

    private void handleMember(Player player, String label, String[] args) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        if (args.length < 4) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " member add|remove <名称> <玩家>"));
            return;
        }
        Region region = manager.findOwnedByName(player, args[2]);
        if (region == null) {
            send(player, "region-not-found", Map.of("name", args[2]));
            return;
        }
        UUID target = resolveTarget(args[3]);
        if (target == null) {
            send(player, "member-not-found", Map.of("player", args[3]));
            return;
        }
        if ("add".equalsIgnoreCase(args[1])) {
            RegionResult result = manager.addMember(region, target, config.defaultMemberMask());
            switch (result) {
                case OK -> send(player, "member-added", Map.of("player", args[3]));
                case MEMBER_EXISTS -> send(player, "member-already", null);
                case MEMBER_IS_OWNER -> send(player, "member-is-owner", null);
                case MEMBER_LIMIT -> send(player, "member-limit", Map.of("max", String.valueOf(config.maxMembers())));
                default -> send(player, "member-not-found", Map.of("player", args[3]));
            }
        } else if ("remove".equalsIgnoreCase(args[1])) {
            RegionResult result = manager.removeMember(region, target);
            if (result == RegionResult.OK) {
                send(player, "member-removed", Map.of("player", args[3]));
            } else {
                send(player, "member-not-found", Map.of("player", args[3]));
            }
        } else {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " member add|remove <名称> <玩家>"));
        }
    }

    private void handlePerm(Player player, String label, String[] args) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        if (args.length < 5) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " perm <名称> <玩家> <权限> <true|false>"));
            return;
        }
        Region region = manager.findOwnedByName(player, args[1]);
        if (region == null) {
            send(player, "region-not-found", Map.of("name", args[1]));
            return;
        }
        UUID target = resolveTarget(args[2]);
        if (target == null || region.member(target) == null) {
            send(player, "member-not-found", Map.of("player", args[2]));
            return;
        }
        PermissionFlag flag = PermissionFlag.byName(args[3]);
        if (flag == null) {
            send(player, "invalid-flag", Map.of("flag", args[3]));
            return;
        }
        boolean value = Boolean.parseBoolean(args[4]);
        manager.setMemberPermission(region, target, flag, value);
        send(player, value ? "perm-granted" : "perm-revoked", Map.of("player", args[2], "flag", flag.name()));
    }

    private void handleSetting(Player player, String label, String[] args) {
        if (!hasUse(player)) {
            send(player, "no-permission", null);
            return;
        }
        if (args.length < 4) {
            send(player, "invalid-usage", Map.of("usage", "/" + label + " setting <名称> <设置> <true|false>"));
            return;
        }
        Region region = manager.findOwnedByName(player, args[1]);
        if (region == null) {
            send(player, "region-not-found", Map.of("name", args[1]));
            return;
        }
        SettingFlag flag = SettingFlag.byName(args[2]);
        if (flag == null) {
            send(player, "invalid-flag", Map.of("flag", args[2]));
            return;
        }
        boolean value = Boolean.parseBoolean(args[3]);
        manager.setSetting(region, flag, value);
        send(player, value ? "setting-enabled" : "setting-disabled", Map.of("flag", flag.name()));
    }

    private void handleSubRegion(CommandSender sender, String label, String[] args) {
        if (!hasAdmin(sender)) {
            send(sender, "no-permission", null);
            return;
        }
        if (args.length < 2) {
            send(sender, "invalid-usage", Map.of("usage",
                    "/" + label + " subregion create <领地> <名称> <offX> <offY> <offZ> <区块X> <段Y> <区块Z>"));
            return;
        }
        String action = args[1].toLowerCase();
        if ("create".equals(action)) {
            if (args.length < 11) {
                send(sender, "invalid-usage", Map.of("usage",
                        "/" + label + " subregion create <领地> <名称> <offX> <offY> <offZ> <区块X> <段Y> <区块Z>"));
                return;
            }
            Region parent = manager.findByName(args[2]);
            if (parent == null) {
                send(sender, "region-not-found", Map.of("name", args[2]));
                return;
            }
            int[] values = new int[6];
            try {
                for (int i = 0; i < 6; i++) {
                    values[i] = Integer.parseInt(args[4 + i]);
                }
            } catch (NumberFormatException e) {
                send(sender, "invalid-amount", null);
                return;
            }
            SubRegion sub = manager.createSubRegion(parent, args[3],
                    values[0], values[1], values[2], values[3], values[4], values[5]);
            if (sub == null) {
                send(sender, "subregion-failed", null);
            } else {
                send(sender, "subregion-created", Map.of("name", sub.name()));
            }
        } else if ("remove".equals(action)) {
            if (args.length < 4) {
                send(sender, "invalid-usage", Map.of("usage", "/" + label + " subregion remove <领地> <名称>"));
                return;
            }
            Region parent = manager.findByName(args[2]);
            if (parent == null) {
                send(sender, "region-not-found", Map.of("name", args[2]));
                return;
            }
            SubRegion sub = manager.findSubRegion(parent, args[3]);
            if (sub == null) {
                send(sender, "region-not-found", Map.of("name", args[3]));
                return;
            }
            manager.deleteSubRegion(sub);
            send(sender, "subregion-removed", Map.of("name", sub.name()));
        } else if ("list".equals(action)) {
            if (args.length < 3) {
                send(sender, "invalid-usage", Map.of("usage", "/" + label + " subregion list <领地>"));
                return;
            }
            Region parent = manager.findByName(args[2]);
            if (parent == null) {
                send(sender, "region-not-found", Map.of("name", args[2]));
                return;
            }
            java.util.List<SubRegion> subs = manager.subRegionsOf(parent);
            if (subs.isEmpty()) {
                send(sender, "subregion-list-empty", null);
                return;
            }
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    config.getMessage("prefix") + config.getMessage("subregion-list-header", Map.of("name", parent.name()))));
            for (SubRegion sub : subs) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<gray> · <white>" + sub.name() + " <dark_gray>" + sub.boundsString()));
            }
        } else {
            send(sender, "invalid-usage", Map.of("usage", "/" + label + " subregion create|remove|list"));
        }
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
                send(sender, "member-not-found", Map.of("player", args[1]));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            send(sender, "invalid-usage", Map.of("usage", "/" + label + " open <玩家>"));
            return;
        }
        openMenu(target);
    }

    private void withPlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
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

    private boolean hasUse(CommandSender sender) {
        return sender.hasPermission("worldme.territory.use") || !(sender instanceof Player);
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission(RegionManager.ADMIN_PERMISSION) || !(sender instanceof Player);
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
            return switch (sub) {
                case "expand" -> DIRECTIONS;
                case "open" -> onlineNames();
                case "member" -> List.of("add", "remove");
                case "subregion" -> List.of("create", "remove", "list");
                case "tp", "sethome", "unclaim", "name", "enter", "leave", "perm", "setting" ->
                        ownedRegionNames(sender);
                default -> Collections.emptyList();
            };
        }

        if (args.length == 3) {
            return switch (sub) {
                case "subregion" -> allRegionNames();
                case "expand", "member", "perm", "setting" -> ownedRegionNames(sender);
                default -> Collections.emptyList();
            };
        }

        if (args.length == 4) {
            if ("perm".equals(sub)) {
                return permissionFlagNames();
            }
            if ("setting".equals(sub)) {
                return settingFlagNames();
            }
            if ("member".equals(sub)) {
                return onlineNames();
            }
            if ("subregion".equals(sub)) {
                String action = args[1].toLowerCase();
                if ("remove".equals(action) || "list".equals(action)) {
                    Region parent = manager.findByName(args[2]);
                    if (parent != null) {
                        List<String> names = new ArrayList<>();
                        for (SubRegion child : manager.subRegionsOf(parent)) {
                            names.add(child.name());
                        }
                        return names;
                    }
                }
            }
            return Collections.emptyList();
        }

        if (args.length == 5 && ("perm".equals(sub) || "setting".equals(sub))) {
            return List.of("true", "false");
        }
        return Collections.emptyList();
    }

    private List<String> permissionFlagNames() {
        List<String> flags = new ArrayList<>();
        for (PermissionFlag flag : PermissionFlag.values()) {
            flags.add(flag.name());
        }
        return flags;
    }

    private List<String> settingFlagNames() {
        List<String> flags = new ArrayList<>();
        for (SettingFlag flag : SettingFlag.values()) {
            flags.add(flag.name());
        }
        return flags;
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private List<String> ownedRegionNames(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (Region region : manager.getByOwner(OwnerType.PLAYER, player.getUniqueId())) {
            names.add(region.name());
        }
        return names;
    }

    private List<String> allRegionNames() {
        List<String> names = new ArrayList<>();
        for (Region region : manager.all()) {
            names.add(region.name());
        }
        return names;
    }
}
