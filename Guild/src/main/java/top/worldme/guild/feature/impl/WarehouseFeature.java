package top.worldme.guild.feature.impl;

import org.bukkit.entity.Player;
import top.worldme.Guild;
import top.worldme.guild.data.GuildFeature;
import top.worldme.guild.feature.GuildFeatureProvider;
import top.worldme.guild.feature.UnlockType;
import top.worldme.guild.gui.WarehouseMenu;

/**
 * 仓库功能：解锁后，成员可打开公会共享仓库。
 */
public class WarehouseFeature implements GuildFeatureProvider {

    private final Guild plugin;

    public WarehouseFeature(Guild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String key() {
        return "warehouse";
    }

    @Override
    public String displayName() {
        return "仓库";
    }

    @Override
    public UnlockType unlockType() {
        return UnlockType.STRUCTURE;
    }

    @Override
    public String structureKey() {
        return "guild-warehouse";
    }

    @Override
    public int requiredLevel() {
        return 2;
    }

    @Override
    public boolean open(top.worldme.guild.data.Guild guild, Player player) {
        GuildFeature feature = guild.feature(key());
        if (feature == null || !feature.unlocked() || !feature.enabled()) {
            plugin.getGuildManager().notify(player.getUniqueId(),
                    plugin.getGuildConfig().getMessage("feature-locked"));
            return true;
        }
        new WarehouseMenu(plugin, player, guild, feature).openFor();
        return true;
    }
}
