package top.worldme.market.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import top.worldme.Market;

public class MarketMenuListener implements Listener {

    private final Market plugin;

    public MarketMenuListener(Market plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MarketMenu menu)) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        if (clicked.equals(event.getInventory())) {
            if (menu.isEditableSlot(event.getSlot())) {
                event.setCancelled(false);
                return;
            }
            event.setCancelled(true);
            menu.handleClick(event);
        } else {
            if (menu.allowPlayerInventoryClick() && !event.isShiftClick()) {
                event.setCancelled(false);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MarketMenu menu)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < event.getInventory().getSize() && !menu.isEditableSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MarketMenu menu) {
            menu.onClose();
        }
    }
}
