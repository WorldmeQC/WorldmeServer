package top.worldme.market.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record MarketListing(int id, UUID seller, ItemStack item, int amount,
                            double price, long listedAt, long expireAt) {
}
