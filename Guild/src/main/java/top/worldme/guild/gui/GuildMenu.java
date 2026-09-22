package top.worldme.guild.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Guild;
import top.worldme.guild.manager.GuildManager;
import top.worldme.territory.data.Region;

import java.util.Map;

/**
 * 公会主界面。
 */
public class GuildMenu extends GuildMenuBase {

    public GuildMenu(Guild plugin, Player player) {
        super(plugin, player, plugin.getMenuConfig().mainTitle, plugin.getMenuConfig().mainRows);
        render();
    }

    @Override
    public void render() {
        top.worldme.guild.data.Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            renderNoGuild();
        } else {
            renderGuild(guild);
        }
        applyBackground();
    }

    private void renderNoGuild() {
        place(menu.infoSlot, menu.infoItem, Map.of(
                "name", "无",
                "leader", "-",
                "level", "0",
                "members", "0",
                "balance", manager.vault().format(0),
                "join_fee", "-",
                "join_mode", "-"));
        place(menu.createSlot, menu.createButton, Map.of(
                "cost", manager.vault().format(manager.config().createCost())));
        place(menu.joinSlot, menu.joinButton, Map.of());
        place(menu.closeSlot, menu.closeButton, Map.of());
    }

    private void renderGuild(top.worldme.guild.data.Guild guild) {
        place(menu.infoSlot, menu.infoItem, Map.of(
                "name", guild.name(),
                "leader", nameOf(guild.leader()),
                "level", String.valueOf(guild.level()),
                "members", guild.memberCount() + "/" + manager.memberCap(guild),
                "balance", manager.vault().format(guild.balance()),
                "join_fee", manager.vault().format(guild.joinFee()),
                "join_mode", guild.openJoin() ? "<green>自由加入" : "<yellow>审批加入"));
        place(menu.depositSlot, menu.depositButton, Map.of());
        place(menu.withdrawSlot, menu.withdrawButton, Map.of());
        place(menu.upgradeSlot, menu.upgradeButton, Map.of(
                "level", String.valueOf(guild.level()),
                "max_level", String.valueOf(manager.config().maxLevel()),
                "upgrade_cost", manager.vault().format(manager.config().upgradeCostFor(guild.level()))));
        place(menu.membersSlot, menu.membersButton, Map.of("members", String.valueOf(guild.memberCount())));
        place(menu.tpSlot, menu.tpButton, Map.of());
        if (guild.leader().equals(player.getUniqueId())) {
            place(menu.disbandSlot, menu.disbandButton, Map.of());
        } else {
            place(menu.leaveSlot, menu.leaveButton, Map.of());
        }
        place(menu.closeSlot, menu.closeButton, Map.of());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == menu.closeSlot) {
            player.closeInventory();
            return;
        }
        top.worldme.guild.data.Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            handleNoGuild(slot);
            return;
        }
        if (slot == menu.depositSlot) {
            handleDeposit();
        } else if (slot == menu.withdrawSlot) {
            handleWithdraw();
        } else if (slot == menu.upgradeSlot) {
            handleUpgrade(guild);
        } else if (slot == menu.membersSlot) {
            open(new GuildMembersMenu(plugin, player));
        } else if (slot == menu.tpSlot) {
            handleTeleport(guild);
        } else if (slot == menu.leaveSlot && !guild.leader().equals(player.getUniqueId())) {
            handleLeave();
        } else if (slot == menu.disbandSlot && guild.leader().equals(player.getUniqueId())) {
            handleDisband(event);
        }
    }

    private void handleNoGuild(int slot) {
        if (slot == menu.createSlot) {
            showTextDialog(menu.createDialog, "", value -> {
                if (value == null || value.isBlank()) {
                    return;
                }
                String name = value.trim();
                switch (manager.create(player, name)) {
                    case OK -> sendMessage("create-success", Map.of("name", name));
                    case IN_GUILD -> sendMessage("create-in-guild", null);
                    case NAME_TAKEN -> sendMessage("create-name-taken", null);
                    case NAME_INVALID -> sendMessage("create-name-invalid", null);
                    case NO_MONEY -> sendMessage("create-no-money", Map.of(
                            "cost", manager.vault().format(manager.config().createCost())));
                    case REGION_UNAVAILABLE -> sendMessage("region-unavailable", null);
                    default -> sendMessage("create-failed", null);
                }
            }, this::reopen);
        } else if (slot == menu.joinSlot) {
            showTextDialog(menu.joinDialog, "", value -> {
                if (value == null || value.isBlank()) {
                    return;
                }
                String name = value.trim();
                top.worldme.guild.data.Guild target = manager.guildByName(name);
                if (target == null) {
                    sendMessage("guild-not-found", Map.of("name", name));
                    return;
                }
                GuildManager.GuildResult result = target.openJoin()
                        ? manager.join(player, target)
                        : manager.requestJoin(player, target);
                switch (result) {
                    case OK -> sendMessage(target.openJoin() ? "join-success" : "join-requested",
                            Map.of("name", target.name(), "fee", manager.vault().format(target.joinFee())));
                    case OPEN_JOIN -> sendMessage("join-open", null);
                    case IN_GUILD -> sendMessage("create-in-guild", null);
                    case NO_MONEY -> sendMessage("join-no-money", Map.of(
                            "fee", manager.vault().format(target.joinFee())));
                    case REJOIN_COOLDOWN -> sendMessage("join-cooldown", null);
                    case ALREADY_APPLIED -> sendMessage("join-already", null);
                    case MEMBER_LIMIT -> sendMessage("member-limit", Map.of(
                            "max", String.valueOf(manager.memberCap(target))));
                    default -> sendMessage("guild-not-found", Map.of("name", name));
                }
            }, this::reopen);
        }
    }

    private void handleDeposit() {
        showTextDialog(menu.amountDialog, "", value -> {
            Double amount = parse(value);
            if (amount == null || amount <= 0) {
                sendMessage("amount-invalid", null);
                return;
            }
            if (manager.deposit(player.getUniqueId(), amount)) {
                sendMessage("deposit-success", Map.of("amount", manager.vault().format(amount)));
            } else {
                sendMessage("deposit-fail", null);
            }
        }, this::reopen);
    }

    private void handleWithdraw() {
        showTextDialog(menu.amountDialog, "", value -> {
            Double amount = parse(value);
            if (amount == null || amount <= 0) {
                sendMessage("amount-invalid", null);
                return;
            }
            if (manager.withdraw(player, amount)) {
                sendMessage("withdraw-success", Map.of("amount", manager.vault().format(amount)));
            } else {
                sendMessage("withdraw-fail", null);
            }
        }, this::reopen);
    }

    private void handleUpgrade(top.worldme.guild.data.Guild guild) {
        switch (manager.upgrade(player)) {
            case OK -> sendMessage("upgrade-success", Map.of("level", String.valueOf(guild.level())));
            case MAX_LEVEL -> sendMessage("upgrade-max", null);
            case NO_MONEY -> sendMessage("upgrade-no-money", Map.of(
                    "cost", manager.vault().format(manager.config().upgradeCostFor(guild.level()))));
            case NOT_LEADER -> sendMessage("disband-owner-only", null);
            default -> sendMessage("upgrade-failed", null);
        }
        refresh();
    }

    private void handleTeleport(top.worldme.guild.data.Guild guild) {
        Region region = manager.regionOf(guild);
        if (region == null || !region.hasWarp() || region.getWarp() == null) {
            sendMessage("tp-no-region", null);
            return;
        }
        player.closeInventory();
        player.teleport(region.getWarp());
        sendMessage("tp-success", null);
    }

    private void handleLeave() {
        switch (manager.leave(player)) {
            case OK -> {
                sendMessage("leave-success", null);
                player.closeInventory();
            }
            case IS_LEADER -> sendMessage("leave-owner", null);
            case TOO_SOON -> sendMessage("leave-too-soon", Map.of(
                    "days", String.valueOf(manager.config().noLeaveDays())));
            default -> sendMessage("no-guild", null);
        }
    }

    private void handleDisband(InventoryClickEvent event) {
        if (!event.isShiftClick() || !event.isLeftClick()) {
            sendMessage("disband-confirm", null);
            return;
        }
        switch (manager.disband(player)) {
            case OK -> {
                sendMessage("disband-success", null);
                player.closeInventory();
            }
            case NOT_LEADER -> sendMessage("disband-owner-only", null);
            default -> sendMessage("no-guild", null);
        }
    }

    private void reopen() {
        open(new GuildMenu(plugin, player));
    }

    private Double parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
