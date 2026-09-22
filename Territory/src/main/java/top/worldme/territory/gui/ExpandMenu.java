package top.worldme.territory.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Territory;
import top.worldme.territory.data.Direction;
import top.worldme.territory.data.Region;
import top.worldme.territory.manager.RegionManager.RegionResult;

import java.util.HashMap;
import java.util.Map;

public class ExpandMenu extends TerritoryMenu {

    private final Region region;
    private final Map<Integer, Direction> slotMap = new HashMap<>();

    public ExpandMenu(Territory plugin, Player player, Region region) {
        super(plugin, player, plugin.getMenuConfig().expandTitle.replace("%name%", region.name()),
                plugin.getMenuConfig().expandRows);
        this.region = region;
        render();
    }

    @Override
    public void render() {
        inventory.clear();
        slotMap.clear();

        int step = menu.expandStep;
        Map<String, String> info = Map.of(
                "volume", String.valueOf(region.volumeUnits()),
                "unit_cost", manager.vault().format(plugin.getTerritoryConfig().perUnitCost()));
        place(menu.expandInfoSlot, menu.expandInfo, info);

        bind(menu.expandNorthSlot, menu.expandNorth, Direction.NORTH, step, info);
        bind(menu.expandSouthSlot, menu.expandSouth, Direction.SOUTH, step, info);
        bind(menu.expandWestSlot, menu.expandWest, Direction.WEST, step, info);
        bind(menu.expandEastSlot, menu.expandEast, Direction.EAST, step, info);
        bind(menu.expandUpSlot, menu.expandUp, Direction.UP, step, info);
        bind(menu.expandDownSlot, menu.expandDown, Direction.DOWN, step, info);

        place(menu.expandBackSlot, menu.expandBack, null);
        applyBackground();
    }

    private void bind(int slot, top.worldme.territory.config.TerritoryMenuConfig.ItemConfig item,
                      Direction direction, int step, Map<String, String> base) {
        Map<String, String> placeholders = new HashMap<>(base);
        placeholders.put("step", String.valueOf(step));
        placeholders.put("cost", manager.vault().format(manager.expandCost(region, direction, step)));
        place(slot, item, placeholders);
        slotMap.put(slot, direction);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.expandBackSlot) {
            open(new RegionMenu(plugin, player, region));
            return;
        }
        Direction direction = slotMap.get(slot);
        if (direction == null) {
            return;
        }
        int units = event.isShiftClick() ? menu.expandStep * 2 : menu.expandStep;
        double cost = manager.expandCost(region, direction, units);
        RegionResult result = manager.expand(player, region, direction, units);
        switch (result) {
            case OK -> sendMessage("expand-success", Map.of(
                    "direction", direction.name(),
                    "units", String.valueOf(units),
                    "cost", manager.vault().format(cost)));
            case LIMIT -> sendMessage("expand-limit", null);
            case WORLD_BOUND -> sendMessage("expand-world-bound", null);
            case OVERLAP -> sendMessage("expand-overlap", null);
            case NO_MONEY -> sendMessage("expand-no-money", Map.of("cost", manager.vault().format(cost)));
            case ECONOMY -> sendMessage("claim-economy", null);
            case NOT_OWNER -> sendMessage("expand-not-owner", null);
            default -> sendMessage("claim-overlap", null);
        }
        inventory.clear();
        render();
    }
}
