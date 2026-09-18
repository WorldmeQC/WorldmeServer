package top.worldme.mail.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.mail.config.MailConfig;
import top.worldme.mail.manager.MailManager;

import java.util.Map;

public class MailListener implements Listener {

    private final JavaPlugin plugin;
    private final MailConfig config;
    private final MailManager mailManager;

    public MailListener(JavaPlugin plugin, MailConfig config, MailManager mailManager) {
        this.plugin = plugin;
        this.config = config;
        this.mailManager = mailManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.unreadReminder()) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            int unread = mailManager.countUnread(player.getUniqueId());
            if (unread > 0) {
                String text = config.getMessage("unread-reminder", Map.of("count", String.valueOf(unread)));
                if (!text.isEmpty()) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
                }
            }
        }, 20L);
    }
}