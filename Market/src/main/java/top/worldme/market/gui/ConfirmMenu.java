package top.worldme.market.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.worldme.Market;
import top.worldme.market.data.MarketListing;
import top.worldme.market.manager.MarketManager.MarketResult;

import java.util.Map;

public class ConfirmMenu extends MarketMenu {

    public enum Action {
        BUY,
        REMOVE,
        FORCE
    }

    private final Action action;
    private final MarketListing listing;
    private final boolean mine;
    private final int returnPage;

    public ConfirmMenu(Market plugin, Player player, Action action, MarketListing listing,
                       boolean mine, int returnPage) {
        super(plugin, player, title(plugin, action), plugin.getMenuConfig().confirmRows);
        this.action = action;
        this.listing = listing;
        this.mine = mine;
        this.returnPage = returnPage;
        render();
    }

    private static String title(Market plugin, Action action) {
        return switch (action) {
            case BUY -> plugin.getMenuConfig().buyTitle;
            case REMOVE -> plugin.getMenuConfig().removeTitle;
            case FORCE -> plugin.getMenuConfig().forceTitle;
        };
    }

    @Override
    public void render() {
        inventory.clear();

        if (menu.confirmItemSlot >= 0 && menu.confirmItemSlot < inventory.getSize()) {
            ItemStack icon = listing.item().clone();
            icon.setAmount(Math.max(1, listing.amount()));
            inventory.setItem(menu.confirmItemSlot, icon);
        }

        Map<String, String> placeholders = Map.of(
                "price", plugin.getEconomy().format(listing.price()),
                "item", top.worldme.market.manager.MarketManager.displayName(listing.item())
        );
        switch (action) {
            case BUY -> place(menu.confirmSlot, menu.buyButton, placeholders);
            case REMOVE -> place(menu.confirmSlot, menu.removeButton, placeholders);
            case FORCE -> place(menu.confirmSlot, menu.forceButton, placeholders);
        }
        place(menu.confirmCancelSlot, menu.confirmCancelButton, placeholders);
        applyBackground();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.confirmCancelSlot) {
            open(new ListingMenu(plugin, player, mine, returnPage));
            return;
        }
        if (slot != menu.confirmSlot) {
            return;
        }

        MarketResult result = switch (action) {
            case BUY -> manager.buy(player, listing);
            case REMOVE -> manager.remove(player, listing);
            case FORCE -> manager.forceRemove(listing);
        };

        Map<String, String> placeholders = Map.of(
                "price", plugin.getEconomy().format(listing.price()),
                "item", top.worldme.market.manager.MarketManager.displayName(listing.item())
        );
        handleResult(result, placeholders);
        player.closeInventory();
    }

    private void handleResult(MarketResult result, Map<String, String> placeholders) {
        String key = switch (result) {
            case OK -> switch (action) {
                case BUY -> "bought";
                case REMOVE -> "removed";
                case FORCE -> "force-removed";
            };
            case NO_MONEY -> "no-money";
            case IS_MINE -> "is-mine";
            case NOT_MINE -> "not-mine";
            case SALED -> "saled";
            case MAIL_ERROR -> "mail-error";
            case ECONOMY_ERROR -> "economy-error";
            default -> "error";
        };
        sendMessage(key, placeholders);
    }
}
