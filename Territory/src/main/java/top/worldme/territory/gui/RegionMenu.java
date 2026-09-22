package top.worldme.territory.gui;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Territory;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.Region;
import top.worldme.territory.manager.RegionManager.RegionResult;

import java.util.Map;

public class RegionMenu extends TerritoryMenu {

    private final Region region;

    public RegionMenu(Territory plugin, Player player, Region region) {
        super(plugin, player, plugin.getMenuConfig().detailTitle.replace("%name%", region.name()),
                plugin.getMenuConfig().detailRows);
        this.region = region;
        render();
    }

    @Override
    public void render() {
        inventory.clear();
        Map<String, String> placeholders = regionPlaceholders(region);
        place(menu.detailInfoSlot, menu.detailInfo, placeholders);

        boolean canManage = manager.isOwnerOrAdmin(player, region);
        if (canManage) {
            place(menu.detailExpandSlot, menu.detailExpand, placeholders);
            place(menu.detailSethomeSlot, menu.detailSethome, placeholders);
            place(menu.detailEnterSlot, menu.detailEnter, placeholders);
            place(menu.detailLeaveSlot, menu.detailLeave, placeholders);
            place(menu.detailNameSlot, menu.detailName, placeholders);
            place(menu.detailMembersSlot, menu.detailMembers, placeholders);
            place(menu.detailSettingsSlot, menu.detailSettings, placeholders);
            place(menu.detailUnclaimSlot, menu.detailUnclaim, placeholders);
        }
        if (region.hasWarp() && manager.canInteract(player, region, PermissionFlag.TELEPORT)) {
            place(menu.detailTeleportSlot, menu.detailTeleport, placeholders);
        }
        place(menu.detailBackSlot, menu.detailBack, placeholders);
        applyBackground();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.detailBackSlot) {
            open(new RegionListMenu(plugin, player));
            return;
        }
        if (slot == menu.detailTeleportSlot && region.hasWarp()
                && manager.canInteract(player, region, PermissionFlag.TELEPORT)) {
            Location warp = region.getWarp();
            if (warp != null) {
                player.closeInventory();
                player.teleport(warp);
                sendMessage("tp-success", Map.of("name", region.name()));
            }
            return;
        }

        boolean canManage = manager.isOwnerOrAdmin(player, region);
        if (!canManage) {
            return;
        }

        if (slot == menu.detailExpandSlot) {
            open(new ExpandMenu(plugin, player, region));
        } else if (slot == menu.detailSethomeSlot) {
            if (!region.contains(player.getLocation())) {
                sendMessage("sethome-outside", null);
                return;
            }
            manager.setHome(region, player.getLocation());
            sendMessage("sethome-success", null);
            refresh();
        } else if (slot == menu.detailEnterSlot) {
            showTextDialog(menu.enterDialog, null, value -> manager.setEnterMessage(region, value),
                    () -> {
                        if (player.isOnline()) {
                            open(new RegionMenu(plugin, player, region));
                        }
                    });
        } else if (slot == menu.detailLeaveSlot) {
            showTextDialog(menu.leaveDialog, null, value -> manager.setLeaveMessage(region, value),
                    () -> {
                        if (player.isOnline()) {
                            open(new RegionMenu(plugin, player, region));
                        }
                    });
        } else if (slot == menu.detailNameSlot) {
            showTextDialog(menu.nameDialog, region.name(), value -> {
                if (value != null && !value.isBlank()) {
                    manager.rename(region, value.trim());
                }
            }, () -> {
                if (player.isOnline()) {
                    open(new RegionMenu(plugin, player, region));
                }
            });
        } else if (slot == menu.detailMembersSlot) {
            open(new MemberMenu(plugin, player, region));
        } else if (slot == menu.detailSettingsSlot) {
            open(new SettingsMenu(plugin, player, region));
        } else if (slot == menu.detailUnclaimSlot && event.isShiftClick()) {
            RegionResult result = manager.unclaim(player, region);
            if (result == RegionResult.OK) {
                sendMessage("unclaim-success", Map.of("name", region.name()));
                open(new RegionListMenu(plugin, player));
            } else if (result == RegionResult.NOT_OWNER) {
                sendMessage("region-not-owner", null);
            }
        }
    }
}
