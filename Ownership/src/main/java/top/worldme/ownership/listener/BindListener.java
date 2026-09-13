package top.worldme.ownership.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import top.worldme.ownership.binding.BindManager;
import top.worldme.ownership.config.OwnerConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BindListener implements Listener {

    private final OwnerConfig config;
    private final BindManager bindManager;
    private final Map<UUID, Long> pickupDenyCooldown = new HashMap<>();

    public BindListener(OwnerConfig config, BindManager bindManager) {
        this.config = config;
        this.bindManager = bindManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (!config.keepOnDeath()) {
            return;
        }
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        List<ItemStack> keeps = new ArrayList<>();
        event.getDrops().removeIf(drop -> {
            if (drop != null && !drop.getType().isAir() && bindManager.isOwnedBy(drop, uuid)) {
                keeps.add(drop);
                return true;
            }
            return false;
        });
        if (!keeps.isEmpty()) {
            event.getItemsToKeep().addAll(keeps);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (!bindManager.isBound(item)) {
            return;
        }
        String ownerUuid = bindManager.getOwnerUuid(item);
        Entity entity = event.getEntity();
        if (entity instanceof Player player && player.getUniqueId().toString().equals(ownerUuid)) {
            return;
        }
        event.setCancelled(true);
        if (config.notifyOnPickupDeny() && entity instanceof Player player) {
            long now = System.currentTimeMillis();
            Long last = pickupDenyCooldown.get(player.getUniqueId());
            if (last != null && now - last < 1000L) {
                return;
            }
            pickupDenyCooldown.put(player.getUniqueId(), now);
            String ownerName = bindManager.getOwnerName(item);
            if (ownerName == null) {
                ownerName = "未知";
            }
            String text = config.getMessage("pickup-denied", Map.of("owner", ownerName));
            if (!text.isEmpty()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
            }
        }
    }
}
