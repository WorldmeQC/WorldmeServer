package top.worldme.guild.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class GuildMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuildMenuBase menu)) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            event.setCancelled(true);
            return;
        }
        if (clicked.equals(event.getInventory())) {
            if (menu.isEditableSlot(event.getRawSlot())) {
                return;
            }
            event.setCancelled(true);
            menu.handleClick(event);
        } else {
            if (menu.allowPlayerInventoryClick()) {
                event.setCancelled(false);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuildMenuBase menu)) {
            return;
        }
        int topSize = event.getInventory().getSize();
        boolean allow = true;
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                if (!menu.isEditableSlot(slot)) {
                    allow = false;
                    break;
                }
            } else if (!menu.allowPlayerInventoryClick()) {
                allow = false;
                break;
            }
        }
        event.setCancelled(!allow);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuildMenuBase menu) {
            menu.onClose();
        }
    }
}
