package top.worldme.market.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.worldme.Market;
import top.worldme.market.config.MarketMenuConfig;
import top.worldme.market.manager.MarketManager.MarketResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellMenu extends MarketMenu {

    private double price = -1;
    private boolean confirmed = false;
    private ItemStack stashed;

    public SellMenu(Market plugin, Player player) {
        super(plugin, player, plugin.getMenuConfig().sellTitle, plugin.getMenuConfig().sellRows);
        render();
    }

    @Override
    public void render() {
        ItemStack stored = inventory.getItem(menu.sellItemSlot);
        inventory.clear();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("fee", price >= 0
                ? plugin.getEconomy().format(plugin.getMarketConfig().listingFee(price))
                : plugin.getMarketConfig().listingFeeDescription());
        placeholders.put("expire_days", String.valueOf(plugin.getMarketConfig().expireDays()));
        placeholders.put("sales_tax", String.valueOf(plugin.getMarketConfig().salesTax()));

        place(menu.sellInfoSlot, menu.sellInfo, placeholders);
        place(menu.sellPriceSlot, menu.sellPriceButton,
                Map.of("price", price >= 0 ? plugin.getEconomy().format(price) : "未设置"));
        place(menu.sellConfirmSlot, menu.sellConfirmButton, placeholders);
        place(menu.sellCancelSlot, menu.sellCancelButton, placeholders);
        if (menu.sellItemHintSlot != menu.sellItemSlot) {
            place(menu.sellItemHintSlot, menu.sellItemHint, placeholders);
        }
        if (stored != null && !stored.getType().isAir()) {
            inventory.setItem(menu.sellItemSlot, stored);
        }
        applyBackground(menu.sellItemSlot);
    }

    @Override
    public boolean isEditableSlot(int slot) {
        return slot == menu.sellItemSlot;
    }

    @Override
    public boolean allowPlayerInventoryClick() {
        return true;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == menu.sellPriceSlot) {
            openPriceDialog();
        } else if (slot == menu.sellConfirmSlot) {
            confirmListing();
        } else if (slot == menu.sellCancelSlot) {
            player.closeInventory();
        }
    }

    @Override
    public void onClose() {
        if (confirmed) {
            return;
        }
        returnItem(inventory.getItem(menu.sellItemSlot));
        inventory.setItem(menu.sellItemSlot, null);
        if (stashed != null) {
            returnItem(stashed);
            stashed = null;
        }
    }

    private void confirmListing() {
        ItemStack item = inventory.getItem(menu.sellItemSlot);
        if (item == null || item.getType().isAir()) {
            sendMessage("need-item", null);
            return;
        }
        if (price < 0) {
            sendMessage("need-price", null);
            return;
        }

        MarketResult result = manager.create(player, item, price);
        switch (result) {
            case OK -> {
                confirmed = true;
                inventory.setItem(menu.sellItemSlot, null);
                double fee = plugin.getMarketConfig().listingFee(price);
                sendMessage("listed", Map.of(
                        "price", plugin.getEconomy().format(price),
                        "fee", plugin.getEconomy().format(fee)
                ));
                player.closeInventory();
            }
            case NO_MONEY -> sendMessage("no-money-fee", Map.of(
                    "fee", plugin.getEconomy().format(plugin.getMarketConfig().listingFee(price))));
            case BLACKLISTED -> sendMessage("blacklisted", null);
            case PRICE_INVALID -> sendMessage("price-invalid", null);
            case TOO_MANY -> sendMessage("too-many", Map.of(
                    "max", String.valueOf(plugin.getMarketConfig().maxListingsPerPlayer())));
            case ECONOMY_ERROR -> sendMessage("economy-error", null);
            default -> sendMessage("error", null);
        }
    }

    private void openPriceDialog() {
        stashItem();
        MarketMenuConfig.DialogConfig cfg = menu.priceDialog;
        DialogInput input = DialogInput.text(
                "price",
                cfg.width,
                mini(cfg.label),
                true,
                price >= 0 ? String.valueOf(price) : "",
                cfg.maxLength,
                null
        );
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mini(cfg.title))
                        .inputs(List.of(input))
                        .canCloseWithEscape(false)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(mini(cfg.confirm), null, 100,
                                DialogAction.customClick((response, audience) -> {
                                    applyPrice(response.getText("price"));
                                    finishDialog();
                                }, ClickCallback.Options.builder().uses(1).build())),
                        ActionButton.create(mini(cfg.cancel), null, 100,
                                DialogAction.customClick((response, audience) -> finishDialog(),
                                        ClickCallback.Options.builder().uses(1).build()))
                )));
        player.showDialog(dialog);
    }

    private void applyPrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < plugin.getMarketConfig().minPrice() || value > plugin.getMarketConfig().maxPrice()) {
                sendMessage("price-invalid", null);
                return;
            }
            this.price = top.worldme.market.config.MarketConfig.round(value);
        } catch (NumberFormatException e) {
            sendMessage("price-invalid", null);
        }
    }

    private void finishDialog() {
        restoreItem();
        render();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.openInventory(inventory);
            }
        });
    }

    private void stashItem() {
        ItemStack item = inventory.getItem(menu.sellItemSlot);
        if (item != null && !item.getType().isAir()) {
            stashed = item;
            inventory.setItem(menu.sellItemSlot, null);
        }
    }

    private void restoreItem() {
        if (stashed != null) {
            inventory.setItem(menu.sellItemSlot, stashed);
            stashed = null;
        }
    }

    private void returnItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
}
