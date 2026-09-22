package top.worldme.territory.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Territory;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.SettingFlag;

import java.util.HashMap;
import java.util.Map;

public class SettingsMenu extends TerritoryMenu {

    private final Region region;
    private final Map<Integer, SettingFlag> slotMap = new HashMap<>();

    public SettingsMenu(Territory plugin, Player player, Region region) {
        super(plugin, player, plugin.getMenuConfig().settingsTitle.replace("%name%", region.name()),
                plugin.getMenuConfig().settingsRows);
        this.region = region;
        render();
    }

    @Override
    public void render() {
        inventory.clear();
        slotMap.clear();

        place(menu.settingsInfoSlot, menu.settingsInfo, Map.of("name", region.name()));

        for (SettingFlag flag : SettingFlag.values()) {
            Integer slot = menu.settingsSlots.get(flag);
            if (slot == null) {
                continue;
            }
            boolean on = region.hasSetting(flag);
            Map<String, String> placeholders = Map.of(
                    "state", on ? "<green>已开启</green>" : "<red>已关闭</red>");
            place(slot, menu.settingsItems.get(flag), placeholders);
            slotMap.put(slot, flag);
        }

        place(menu.settingsBackSlot, menu.settingsBack, null);
        applyBackground();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.settingsBackSlot) {
            open(new RegionMenu(plugin, player, region));
            return;
        }
        SettingFlag flag = slotMap.get(slot);
        if (flag == null) {
            return;
        }
        if (!manager.isOwnerOrAdmin(player, region)) {
            sendMessage("no-permission", null);
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
        manager.setSetting(region, flag, value);
        sendMessage(value ? "setting-enabled" : "setting-disabled", Map.of("flag", flag.name()));
        refresh();
    }
}
