package top.worldme.guild.feature.impl;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.worldme.Guild;
import top.worldme.guild.config.FeatureDefinition;
import top.worldme.guild.data.GuildMember;
import top.worldme.guild.feature.GuildFeatureProvider;
import top.worldme.guild.feature.UnlockType;
import top.worldme.guild.manager.GuildManager;
import top.worldme.territory.data.Region;

import java.util.Locale;

/**
 * 信标功能：解锁后，位于公会领地内的在线成员周期性获得药水效果。
 */
public class BeaconFeature implements GuildFeatureProvider {

    private final Guild plugin;

    public BeaconFeature(Guild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String key() {
        return "beacon";
    }

    @Override
    public String displayName() {
        return "信标";
    }

    @Override
    public UnlockType unlockType() {
        return UnlockType.STRUCTURE;
    }

    @Override
    public String structureKey() {
        return "guild-beacon";
    }

    @Override
    public int requiredLevel() {
        return 1;
    }

    @Override
    public void onTick(top.worldme.guild.data.Guild guild) {
        GuildManager manager = plugin.getGuildManager();
        Region region = manager.regionOf(guild);
        if (region == null) {
            return;
        }
        FeatureDefinition definition = plugin.getStructureConfig().feature(key());
        PotionEffectType type = resolveEffect(definition == null ? "SPEED"
                : String.valueOf(definition.option("effect", "SPEED")));
        if (type == null) {
            return;
        }
        int amplifier = number(definition == null ? 1 : definition.option("amplifier", 1), 1);
        int duration = number(definition == null ? 300 : definition.option("duration", 300), 300);
        for (GuildMember member : guild.memberList()) {
            Player player = Bukkit.getPlayer(member.uuid());
            if (player == null || !player.isOnline() || !region.contains(player.getLocation())) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
        }
    }

    private PotionEffectType resolveEffect(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(normalized.contains(":") ? normalized
                : "minecraft:" + normalized);
        return key == null ? null : Registry.EFFECT.get(key);
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
