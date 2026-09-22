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
        if (event.getInventory().getHolder() instanceof GuildMenuBase) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuildMenuBase menu) {
            menu.onClose();
        }
    }
}
