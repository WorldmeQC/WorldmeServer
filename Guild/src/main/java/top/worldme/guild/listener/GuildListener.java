package top.worldme.guild.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import top.worldme.guild.manager.GuildManager;

public class GuildListener implements Listener {

    private final GuildManager manager;

    public GuildListener(GuildManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.flushNotices(event.getPlayer());
    }
}
