package top.worldme.territory.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.SubRegion;

/**
 * 玩家进入领地（或子区域）时触发。取消可阻止默认的进入标题提示。
 */
public class RegionEnterEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Region region;
    private final SubRegion subRegion;
    private boolean cancelled;

    public RegionEnterEvent(Player player, Region region, SubRegion subRegion) {
        this.player = player;
        this.region = region;
        this.subRegion = subRegion;
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
