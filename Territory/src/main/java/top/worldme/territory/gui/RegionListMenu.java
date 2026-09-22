package top.worldme.territory.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Territory;
import top.worldme.territory.data.OwnerType;
import top.worldme.territory.data.Region;
import top.worldme.territory.manager.RegionManager.RegionResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionListMenu extends TerritoryMenu {

    private final Map<Integer, Region> slotMap = new HashMap<>();
    private int page;

    public RegionListMenu(Territory plugin, Player player) {
        this(plugin, player, 1);
    }

    public RegionListMenu(Territory plugin, Player player, int page) {
        super(plugin, player, plugin.getMenuConfig().listTitle, plugin.getMenuConfig().listRows);
        this.page = Math.max(1, page);
        render();
    }

    @Override
    public void render() {
        inventory.clear();
        slotMap.clear();

        List<Region> regions = manager.getByOwner(OwnerType.PLAYER, player.getUniqueId());
        int perPage = Math.max(1, menu.listContentSlots.size());
        int maxPage = Math.max(1, (regions.size() + perPage - 1) / perPage);
        this.page = Math.min(page, maxPage);

        place(menu.listInfoSlot, menu.listInfo, Map.of(
                "count", String.valueOf(regions.size()),
                "max", String.valueOf(manager.maxClaims(player))));
        place(menu.createSlot, menu.createButton, Map.of(
                "cost", manager.vault().format(plugin.getTerritoryConfig().costForUnits(1))));

        int start = (page - 1) * perPage;
        for (int i = 0; i < perPage; i++) {
            int index = start + i;
            if (index >= regions.size()) {
                break;
            }
            Region region = regions.get(index);
            int slot = menu.listContentSlots.get(i);
            place(slot, menu.listItem, regionPlaceholders(region));
            slotMap.put(slot, region);
        }

        place(menu.listPrevSlot, menu.listPrevButton, null);
        place(menu.listNextSlot, menu.listNextButton, null);
        place(menu.listCloseSlot, menu.listCloseButton, null);
        applyBackground();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.listCloseSlot) {
            player.closeInventory();
            return;
        }
        if (slot == menu.createSlot) {
            RegionResult result = manager.create(player);
            sendResult(result);
            refresh();
            return;
        }
        if (slot == menu.listPrevSlot) {
            if (page > 1) {
                open(new RegionListMenu(plugin, player, page - 1));
            }
            return;
        }
        if (slot == menu.listNextSlot) {
            List<Region> regions = manager.getByOwner(OwnerType.PLAYER, player.getUniqueId());
            int perPage = Math.max(1, menu.listContentSlots.size());
            if (page * perPage < regions.size()) {
                open(new RegionListMenu(plugin, player, page + 1));
            }
            return;
        }
        Region region = slotMap.get(slot);
        if (region != null) {
            open(new RegionMenu(plugin, player, region));
        }
    }

    private void sendResult(RegionResult result) {
        double cost = plugin.getTerritoryConfig().costForUnits(1);
        switch (result) {
            case OK -> sendMessage("claim-success", Map.of(
                    "cost", manager.vault().format(cost),
                    "count", String.valueOf(manager.countByOwner(OwnerType.PLAYER, player.getUniqueId())),
                    "max", String.valueOf(manager.maxClaims(player))));
            case AT_CAP -> sendMessage("claim-at-cap", Map.of("max", String.valueOf(manager.maxClaims(player))));
            case OVERLAP -> sendMessage("claim-overlap", null);
            case WORLD_DENIED -> sendMessage("claim-world-denied", null);
            case WORLD_BOUND -> sendMessage("expand-world-bound", null);
            case NO_MONEY -> sendMessage("claim-no-money", Map.of("cost", manager.vault().format(cost)));
            case ECONOMY -> sendMessage("claim-economy", null);
            default -> sendMessage("claim-overlap", null);
        }
    }
}
