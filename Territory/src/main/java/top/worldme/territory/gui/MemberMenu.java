package top.worldme.territory.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Territory;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.RegionMember;
import top.worldme.territory.manager.RegionManager.RegionResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MemberMenu extends TerritoryMenu {

    private final Region region;
    private final Map<Integer, UUID> slotMap = new HashMap<>();

    public MemberMenu(Territory plugin, Player player, Region region) {
        super(plugin, player, plugin.getMenuConfig().membersTitle.replace("%name%", region.name()),
                plugin.getMenuConfig().membersRows);
        this.region = region;
        render();
    }

    @Override
    public void render() {
        inventory.clear();
        slotMap.clear();

        place(menu.membersInfoSlot, menu.membersInfo, Map.of(
                "name", region.name(),
                "count", String.valueOf(region.members().size())));

        List<RegionMember> members = new ArrayList<>(region.memberList());
        members.sort(Comparator.comparing(m -> name(m.uuid()), String.CASE_INSENSITIVE_ORDER));

        int perPage = Math.max(1, menu.membersContentSlots.size());
        for (int i = 0; i < members.size() && i < perPage; i++) {
            RegionMember member = members.get(i);
            int slot = menu.membersContentSlots.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(member.uuid());
            inventory.setItem(slot, buildHead(offline, menu.membersItem, Map.of("player", name(member.uuid()))));
            slotMap.put(slot, member.uuid());
        }

        place(menu.membersAddSlot, menu.membersAdd, null);
        place(menu.membersBackSlot, menu.membersBack, null);
        applyBackground();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.membersBackSlot) {
            open(new RegionMenu(plugin, player, region));
            return;
        }
        if (slot == menu.membersAddSlot) {
            showTextDialog(menu.memberDialog, null, this::addMember, () -> {
                if (player.isOnline()) {
                    open(new MemberMenu(plugin, player, region));
                }
            });
            return;
        }
        UUID target = slotMap.get(slot);
        if (target != null) {
            open(new MemberPermissionMenu(plugin, player, region, target));
        }
    }

    private void addMember(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        Player online = Bukkit.getPlayerExact(name.trim());
        OfflinePlayer offline;
        if (online != null) {
            offline = online;
        } else {
            offline = Bukkit.getOfflinePlayer(name.trim());
            if (!offline.hasPlayedBefore()) {
                sendMessage("member-not-found", Map.of("player", name.trim()));
                return;
            }
        }
        RegionResult result = manager.addMember(region, offline.getUniqueId(),
                plugin.getTerritoryConfig().defaultMemberMask());
        switch (result) {
            case OK -> sendMessage("member-added", Map.of("player", name(offline.getUniqueId())));
            case MEMBER_EXISTS -> sendMessage("member-already", null);
            case MEMBER_IS_OWNER -> sendMessage("member-is-owner", null);
            case MEMBER_LIMIT -> sendMessage("member-limit", Map.of(
                    "max", String.valueOf(plugin.getTerritoryConfig().maxMembers())));
            default -> sendMessage("member-not-found", Map.of("player", name.trim()));
        }
    }

    private String name(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString().substring(0, 8) : name;
    }
}
