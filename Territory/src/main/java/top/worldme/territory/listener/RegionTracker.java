package top.worldme.territory.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import top.worldme.territory.api.event.RegionEnterEvent;
import top.worldme.territory.api.event.RegionLeaveEvent;
import top.worldme.territory.config.TerritoryConfig;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.SettingFlag;
import top.worldme.territory.data.SubRegion;
import top.worldme.territory.manager.RegionManager;
import top.worldme.territory.util.RegionGeometry;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检测玩家进入/离开领地或子区域，触发事件并显示标题提示。
 */
public class RegionTracker implements Listener {

    private final TerritoryConfig config;
    private final RegionManager manager;
    private final Map<UUID, String> zones = new ConcurrentHashMap<>();

    public RegionTracker(TerritoryConfig config, RegionManager manager) {
        this.config = config;
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (RegionGeometry.chunkOf(from.getBlockX()) == RegionGeometry.chunkOf(to.getBlockX())
                && RegionGeometry.sectionOf(from.getBlockY()) == RegionGeometry.sectionOf(to.getBlockY())
                && RegionGeometry.chunkOf(from.getBlockZ()) == RegionGeometry.chunkOf(to.getBlockZ())) {
            return;
        }
        update(event.getPlayer(), to);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to != null) {
            update(event.getPlayer(), to);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String key = zoneKey(player.getLocation());
        if (key == null) {
            zones.remove(player.getUniqueId());
        } else {
            zones.put(player.getUniqueId(), key);
        }
        applyGlow(player, player.getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        zones.remove(event.getPlayer().getUniqueId());
    }

    private void update(Player player, Location location) {
        String key = zoneKey(location);
        String previous = zones.get(player.getUniqueId());
        if (Objects.equals(key, previous)) {
            applyGlow(player, location);
            return;
        }

        if (previous != null) {
            Zone old = resolve(previous);
            if (old != null && old.region() != null) {
                RegionLeaveEvent event = new RegionLeaveEvent(player, old.region(), old.subRegion());
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    showLeave(player, old.displayName());
                }
            }
        }

        if (key != null) {
            Zone now = resolve(key);
            if (now != null && now.region() != null) {
                RegionEnterEvent event = new RegionEnterEvent(player, now.region(), now.subRegion());
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    showEnter(player, now);
                }
            }
        }

        if (key == null) {
            zones.remove(player.getUniqueId());
        } else {
            zones.put(player.getUniqueId(), key);
        }
        applyGlow(player, location);
    }

    private String zoneKey(Location location) {
        Region region = manager.getRegionAt(location);
        if (region == null) {
            return null;
        }
        SubRegion sub = manager.getSubRegionAt(location);
        return sub != null ? "s" + sub.id() : "r" + region.id();
    }

    private Zone resolve(String key) {
        if (key == null || key.length() < 2) {
            return null;
        }
        try {
            int id = Integer.parseInt(key.substring(1));
            if (key.charAt(0) == 's') {
                SubRegion sub = manager.getSubRegionById(id);
                return sub == null ? null : new Zone(sub.parent(), sub);
            }
            Region region = manager.getById(id);
            return region == null ? null : new Zone(region, null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showEnter(Player player, Zone zone) {
        Region region = zone.region();
        String owner = playerName(region.ownerId());
        String subtitle = region.enterMessage() == null
                ? config.getMessage("enter-subtitle", Map.of("owner", owner))
                : region.enterMessage();
        showTitle(player,
                config.getMessage("enter-title", Map.of("region", zone.displayName())),
                subtitle);
    }

    private void showLeave(Player player, String name) {
        showTitle(player,
                config.getMessage("leave-title", Map.of("region", name)),
                config.getMessage("leave-subtitle", Map.of("region", name)));
    }

    private void showTitle(Player player, String title, String subtitle) {
        if ((title == null || title.isEmpty()) && (subtitle == null || subtitle.isEmpty())) {
            return;
        }
        Component titleComponent = MiniMessage.miniMessage().deserialize(title == null ? "" : title);
        Component subtitleComponent = MiniMessage.miniMessage().deserialize(subtitle == null ? "" : subtitle);
        player.showTitle(Title.title(titleComponent, subtitleComponent,
                Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(1300), Duration.ofMillis(500))));
    }

    private void applyGlow(Player player, Location location) {
        boolean glow = manager.hasSetting(location, SettingFlag.GLOW_PLAYERS);
        if (player.isGlowing() != glow) {
            player.setGlowing(glow);
        }
    }

    private String playerName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "未知" : name;
    }

    private record Zone(Region region, SubRegion subRegion) {
        String displayName() {
            return subRegion != null ? subRegion.name() : region.name();
        }
    }
}
