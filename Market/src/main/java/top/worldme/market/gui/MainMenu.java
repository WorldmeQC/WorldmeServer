package top.worldme.market.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import top.worldme.Market;

import java.util.Map;

public class MainMenu extends MarketMenu {

    public MainMenu(Market plugin, Player player) {
        super(plugin, player, plugin.getMenuConfig().mainTitle, plugin.getMenuConfig().mainRows);
        render();
    }

    @Override
    public void render() {
        inventory.clear();
        Map<String, String> placeholders = Map.of(
                "count", String.valueOf(manager.count()),
                "fee", plugin.getMarketConfig().listingFeeDescription()
        );
        place(menu.mainInfoSlot, menu.mainInfo, placeholders);
        place(menu.browseSlot, menu.browseButton, placeholders);
        place(menu.myListingsSlot, menu.myListingsButton, placeholders);
        place(menu.sellMenuSlot, menu.sellButton, placeholders);
        place(menu.mainCloseSlot, menu.closeButton, placeholders);
        applyBackground();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.browseSlot) {
            open(new ListingMenu(plugin, player, false, 1));
        } else if (slot == menu.myListingsSlot) {
            open(new ListingMenu(plugin, player, true, 1));
        } else if (slot == menu.sellMenuSlot) {
            open(new SellMenu(plugin, player));
        } else if (slot == menu.mainCloseSlot) {
            player.closeInventory();
        }
    }
}
