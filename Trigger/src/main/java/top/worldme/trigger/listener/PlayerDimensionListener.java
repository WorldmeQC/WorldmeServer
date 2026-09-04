package top.worldme.trigger.listener;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.trigger.config.TriggerConfig;
import top.worldme.trigger.data.PlayerUnlockData;

public class PlayerDimensionListener implements Listener {

    private final JavaPlugin plugin;
    private final TriggerConfig triggerConfig;
    private final PlayerUnlockData playerUnlockData;
    private final LuckPerms luckPerms;

    public PlayerDimensionListener(org.bukkit.plugin.java.JavaPlugin plugin,
                                   TriggerConfig triggerConfig,
                                   PlayerUnlockData playerUnlockData,
                                   LuckPerms luckPerms) {
        this.plugin = plugin;
        this.triggerConfig = triggerConfig;
        this.playerUnlockData = playerUnlockData;
        this.luckPerms = luckPerms;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        World.Environment environment = world.getEnvironment();

        TriggerConfig.WorldConfig config = triggerConfig.getWorldConfig(environment);
        if (config == null) {
            return;
        }

        if (playerUnlockData.isUnlocked(player.getUniqueId(), config.getKey())) {
            return;
        }

        if (luckPerms == null) {
            plugin.getLogger().warning("LuckPerms 未加载，无法为玩家 " + player.getName() + " 授予权限。");
            return;
        }

        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
            for (String permission : config.getPermissions()) {
                user.data().add(Node.builder(permission).build());
            }
        });

        playerUnlockData.setUnlocked(player.getUniqueId(), config.getKey());

        if (!config.getActionbarMessage().isEmpty()) {
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(config.getActionbarMessage()));
        }
    }
}
