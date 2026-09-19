package top.worldme.market.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.worldme.Market;
import top.worldme.market.data.MarketListing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListingMenu extends MarketMenu {

    private final boolean mine;
    private int page;
    private int maxPage = 1;

    public ListingMenu(Market plugin, Player player, boolean mine, int page) {
        super(plugin, player,
                mine ? plugin.getMenuConfig().mineTitle : plugin.getMenuConfig().browseTitle,
                plugin.getMenuConfig().listRows);
        this.mine = mine;
        this.page = Math.max(1, page);
        render();
    }

    private int total() {
        return mine ? manager.countBySeller(player.getUniqueId()) : manager.count();
    }

    private List<MarketListing> currentItems() {
        int size = Math.max(1, menu.listContentSlots.size());
        return mine
                ? manager.getBySellerPaged(player.getUniqueId(), page, size)
                : manager.getAllPaged(page, size);
    }

    @Override
    public void render() {
        inventory.clear();
        int pageSize = Math.max(1, menu.listContentSlots.size());
        int total = total();
        maxPage = manager.maxPage(total, pageSize);
        page = Math.max(1, Math.min(page, maxPage));

        Map<String, String> info = Map.of(
                "title", mine ? "我的上架" : "全部商品",
                "count", String.valueOf(total),
                "page", String.valueOf(page),
                "pages", String.valueOf(maxPage)
        );
        place(menu.listInfoSlot, menu.listInfo, info);

        List<MarketListing> items = currentItems();
        for (int i = 0; i < menu.listContentSlots.size(); i++) {
            if (i >= items.size()) {
                break;
            }
            int slot = menu.listContentSlots.get(i);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            inventory.setItem(slot, listingIcon(items.get(i)));
        }

        place(menu.prevSlot, page > 1 ? menu.prevButton : menu.prevButtonDisabled, info);
        place(menu.nextSlot, page < maxPage ? menu.nextButton : menu.nextButtonDisabled, info);
        place(menu.backSlot, menu.backButton, info);
        place(menu.listCloseSlot, menu.listCloseButton, info);
        applyBackground();
    }

    private ItemStack listingIcon(MarketListing listing) {
        ItemStack item = listing.item().clone();
        item.setAmount(Math.max(1, listing.amount()));

        String sellerName = Bukkit.getOfflinePlayer(listing.seller()).getName();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("price", plugin.getEconomy().format(listing.price()));
        placeholders.put("amount", String.valueOf(listing.amount()));
        placeholders.put("seller", sellerName == null ? listing.seller().toString() : sellerName);
        placeholders.put("expire_at", plugin.getMarketConfig().formatTime(listing.expireAt()));
        appendLore(item, menu.listingLore, placeholders);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.prevSlot) {
            if (page > 1) {
                page--;
                render();
            } else {
                sendMessage("first-page", null);
            }
            return;
        }
        if (slot == menu.nextSlot) {
            if (page < maxPage) {
                page++;
                render();
            } else {
                sendMessage("last-page", null);
            }
            return;
        }
        if (slot == menu.backSlot) {
            open(new MainMenu(plugin, player));
            return;
        }
        if (slot == menu.listCloseSlot) {
            player.closeInventory();
            return;
        }

        int index = menu.listContentSlots.indexOf(slot);
        if (index < 0) {
            return;
        }
        ItemStack current = inventory.getItem(slot);
        if (current == null || current.getType().isAir()) {
            return;
        }
        List<MarketListing> items = currentItems();
        if (index >= items.size()) {
            return;
        }
        MarketListing listing = items.get(index);

        if (!mine && event.isRightClick() && player.hasPermission("worldme.market.admin")) {
            open(new ConfirmMenu(plugin, player, ConfirmMenu.Action.FORCE, listing, false, page));
        } else if (mine) {
            open(new ConfirmMenu(plugin, player, ConfirmMenu.Action.REMOVE, listing, true, page));
        } else {
            open(new ConfirmMenu(plugin, player, ConfirmMenu.Action.BUY, listing, false, page));
        }
    }
}
