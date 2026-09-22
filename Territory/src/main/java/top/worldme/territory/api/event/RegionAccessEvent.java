package top.worldme.territory.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.SubRegion;

/**
 * 权限判定事件。默认结果 = 未被取消。
 * 其他模块可通过 {@link #setCancelled(boolean)} 覆盖判定：
 * 取消表示拒绝，取消后再设为 false 表示放行。
 */
public class RegionAccessEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Region region;
    private final SubRegion subRegion;
    private final Location location;
    private final PermissionFlag flag;
    private boolean cancelled;

    public RegionAccessEvent(Player player, Region region, SubRegion subRegion, Location location,
                             PermissionFlag flag, boolean allowed) {
        this.player = player;
        this.region = region;
        this.subRegion = subRegion;
        this.location = location;
        this.flag = flag;
        this.cancelled = !allowed;
    }

    public Player player() {
        return player;
    }

    public Region region() {
        return region;
    }

    public SubRegion subRegion() {
        return subRegion;
    }

    public Location location() {
        return location;
    }

    public PermissionFlag flag() {
        return flag;
    }

    public boolean allowed() {
        return !cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
