package top.worldme.guild.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Guild;
import top.worldme.guild.config.GuildMenuConfig;
import top.worldme.guild.data.GuildMember;
import top.worldme.guild.data.GuildRank;
import top.worldme.guild.manager.GuildManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 公会成员管理界面。
 */
public class GuildMembersMenu extends GuildMenuBase {

    public GuildMembersMenu(Guild plugin, Player player) {
        super(plugin, player, plugin.getMenuConfig().membersTitle, plugin.getMenuConfig().membersRows);
        render();
    }

    @Override
    public void render() {
        top.worldme.guild.data.Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            place(menu.membersBackSlot, menu.membersBack, Map.of());
            applyBackground();
            return;
        }
        List<GuildMember> members = sorted(guild);
        List<Integer> slots = menu.membersContentSlots;
        for (int i = 0; i < members.size() && i < slots.size(); i++) {
            GuildMember member = members.get(i);
            GuildRank rank = guild.rank(member.rankKey());
            String rankName = rank == null ? member.rankKey() : rank.display();
            OfflinePlayer offline = Bukkit.getOfflinePlayer(member.uuid());
            placeHead(slots.get(i), offline, menu.membersItem, Map.of(
                    "player", nameOf(member.uuid()),
                    "rank", rankName,
                    "status", offline.isOnline() ? "<green>在线" : "<gray>离线"));
        }
        place(menu.membersInfoSlot, menu.membersInfo, Map.of(
                "name", guild.name(),
                "count", guild.memberCount() + "/" + manager.memberCap(guild)));
        place(menu.membersBackSlot, menu.membersBack, Map.of());
        applyBackground();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == menu.membersBackSlot) {
            open(new GuildMenu(plugin, player));
            return;
        }
        top.worldme.guild.data.Guild guild = manager.guildOf(player.getUniqueId());
        if (guild == null) {
            return;
        }
        int index = menu.membersContentSlots.indexOf(slot);
        if (index < 0) {
            return;
        }
        List<GuildMember> members = sorted(guild);
        if (index >= members.size()) {
            return;
        }
        GuildMember member = members.get(index);
        if (member.uuid().equals(player.getUniqueId()) || member.uuid().equals(guild.leader())) {
            return;
        }
        if (event.isShiftClick() && event.isLeftClick()) {
            GuildManager.GuildResult result = manager.kick(player, guild, member.uuid());
            if (result == GuildManager.GuildResult.OK) {
                sendMessage("kick-success", Map.of("player", nameOf(member.uuid())));
            } else if (result == GuildManager.GuildResult.NO_PERMISSION) {
                sendMessage("no-permission", null);
            }
        } else if (event.isLeftClick()) {
            applyRank(guild, member, manager.setRank(player, guild, member.uuid(), GuildManager.RANK_OFFICER));
        } else if (event.isRightClick()) {
            applyRank(guild, member, manager.setRank(player, guild, member.uuid(), GuildManager.RANK_MEMBER));
        }
        refresh();
    }

    private void applyRank(top.worldme.guild.data.Guild guild, GuildMember member, GuildManager.GuildResult result) {
        if (result == GuildManager.GuildResult.OK) {
            GuildRank rank = guild.rank(member.rankKey());
            sendMessage("rank-set", Map.of("player", nameOf(member.uuid()),
                    "rank", rank == null ? member.rankKey() : rank.display()));
        } else if (result == GuildManager.GuildResult.NO_PERMISSION) {
            sendMessage("no-permission", null);
        } else if (result == GuildManager.GuildResult.IS_LEADER) {
            sendMessage("disband-owner-only", null);
        }
    }

    private List<GuildMember> sorted(top.worldme.guild.data.Guild guild) {
        List<GuildMember> members = new ArrayList<>(guild.memberList());
        members.sort(Comparator
                .comparingInt((GuildMember member) -> priority(guild, member)).reversed()
                .thenComparing(member -> nameOf(member.uuid()), String.CASE_INSENSITIVE_ORDER));
        return members;
    }

    private int priority(top.worldme.guild.data.Guild guild, GuildMember member) {
        GuildRank rank = guild.rank(member.rankKey());
        return rank == null ? 0 : rank.priority();
    }

    private void placeHead(int slot, OfflinePlayer owner, GuildMenuConfig.ItemConfig item, Map<String, String> placeholders) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        var stack = buildHead(owner, item, placeholders);
        if (stack != null) {
            inventory.setItem(slot, stack);
        }
    }
}
