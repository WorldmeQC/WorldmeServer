package top.worldme.territory.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import top.worldme.territory.config.TerritoryConfig;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.SettingFlag;
import top.worldme.territory.manager.RegionManager;

import java.util.Iterator;

public class ProtectionListener implements Listener {

    private final TerritoryConfig config;
    private final RegionManager manager;

    public ProtectionListener(TerritoryConfig config, RegionManager manager) {
        this.config = config;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (manager.canInteract(player, event.getBlock().getLocation(), PermissionFlag.BREAK)) {
            return;
        }
        event.setCancelled(true);
        deny(player, "deny-break");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (manager.canInteract(player, event.getBlock().getLocation(), PermissionFlag.BUILD)) {
            return;
        }
        event.setCancelled(true);
        deny(player, "deny-place");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        Material type = block.getType();

        PermissionFlag required;
        String denyKey;
        if (isContainer(block)) {
            required = PermissionFlag.USE_CONTAINER;
            denyKey = "deny-container";
        } else if (Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type)) {
            required = PermissionFlag.USE_DOOR;
            denyKey = "deny-interact";
        } else if (Tag.BUTTONS.isTagged(type) || type == Material.LEVER) {
            required = PermissionFlag.USE_BUTTON;
            denyKey = "deny-interact";
        } else {
            return;
        }

        if (!manager.canInteract(player, block.getLocation(), required)) {
            event.setCancelled(true);
            deny(player, denyKey);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (manager.canInteract(player, event.getRightClicked().getLocation(), PermissionFlag.INTERACT_ENTITY)) {
            return;
        }
        event.setCancelled(true);
        deny(player, "deny-interact");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (manager.canInteract(event.getPlayer(), event.getBlock().getLocation(), PermissionFlag.BUILD)) {
            return;
        }
        event.setCancelled(true);
        deny(event.getPlayer(), "deny-place");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (manager.canInteract(event.getPlayer(), event.getBlock().getLocation(), PermissionFlag.BREAK)) {
            return;
        }
        event.setCancelled(true);
        deny(event.getPlayer(), "deny-break");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        SettingFlag setting;
        if (entity instanceof TNTPrimed) {
            setting = SettingFlag.TNT_EXPLODE;
        } else if (entity instanceof Creeper) {
            setting = SettingFlag.CREEPER_EXPLODE;
        } else {
            return;
        }
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!manager.hasSetting(block.getLocation(), setting)) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.SPREAD) {
            return;
        }
        if (!manager.hasSetting(event.getBlock().getLocation(), SettingFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.NATURAL
                && reason != CreatureSpawnEvent.SpawnReason.SPAWNER
                && reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }
        if (!manager.hasSetting(event.getLocation(), SettingFlag.SPAWN)) {
            event.setCancelled(true);
        }
    }

    private boolean isContainer(Block block) {
        if (block.getState() instanceof Container) {
            return true;
        }
        Material type = block.getType();
        return type == Material.ENDER_CHEST || type == Material.DECORATED_POT;
    }

    private void deny(Player player, String key) {
        String text = config.getMessage(key);
        if (text == null || text.isEmpty()) {
            return;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
    }
}
