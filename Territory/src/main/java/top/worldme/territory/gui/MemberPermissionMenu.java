package top.worldme.territory.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Territory;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.RegionMember;
import top.worldme.territory.manager.RegionManager.RegionResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemberPermissionMenu extends TerritoryMenu {

    private final Region region;
    private final UUID target;
    private final Map<Integer, PermissionFlag> slotMap = new HashMap<>();

    public MemberPermissionMenu(Territory plugin, Player player, Region region, UUID target) {
        super(plugin, player, plugin.getMenuConfig().memberPermissionsTitle.replace("%player%", name(target)),
                plugin.getMenuConfig().memberPermissionsRows);
        this.region = region;
        this.target = target;
        render();
    }

    @Override
    public void render() {
        inventory.clear();
        slotMap.clear();

        place(menu.memberPermissionsInfoSlot, menu.memberPermissionsInfo, Map.of("player", name(target)));

        RegionMember member = region.member(target);
        if (member == null) {
            place(menu.memberPermissionsBackSlot, menu.memberPermissionsBack, null);
            applyBackground();
            return;
        }

        for (PermissionFlag flag : PermissionFlag.values()) {
            Integer slot = menu.permissionSlots.get(flag);
            if (slot == null) {
                continue;
            }
            boolean on = member.has(flag);
            Map<String, String> placeholders = Map.of(
                    "state", on ? "<green>已授予</green>" : "<red>未授予</red>");
            place(slot, menu.permissionItems.get(flag), placeholders);
            slotMap.put(slot, flag);
        }

        place(menu.memberPermissionsRemoveSlot, menu.memberPermissionsRemove, null);
        place(menu.memberPermissionsBackSlot, menu.memberPermissionsBack, null);
        applyBackground();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.memberPermissionsBackSlot) {
            open(new MemberMenu(plugin, player, region));
            return;
        }
        if (slot == menu.memberPermissionsRemoveSlot) {
            if (!event.isShiftClick()) {
                return;
            }
            RegionResult result = manager.removeMember(region, target);
            if (result == RegionResult.OK) {
                sendMessage("member-removed", Map.of("player", name(target)));
                open(new MemberMenu(plugin, player, region));
            }
            return;
        }
        PermissionFlag flag = slotMap.get(slot);
        if (flag == null) {
            return;
        }
        boolean value;
        if (event.getClick() == ClickType.LEFT) {
            value = true;
        } else if (event.getClick() == ClickType.RIGHT) {
            value = false;
        } else {
            return;
        }
        manager.setMemberPermission(region, target, flag, value);
        sendMessage(value ? "perm-granted" : "perm-revoked", Map.of(
                "player", name(target),
                "flag", flag.name()));
        refresh();
    }

    private static String name(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString().substring(0, 8) : name;
    }
}
