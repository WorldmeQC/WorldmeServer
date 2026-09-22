package top.worldme.guild.api;

import org.bukkit.entity.Player;
import top.worldme.guild.data.Guild;
import top.worldme.guild.feature.FeatureRegistry;
import top.worldme.guild.feature.GuildFeatureProvider;

import java.util.List;
import java.util.UUID;

/**
 * 公会外部接口，供其他模块调用。
 * 通过 {@code (Guild) Bukkit.getPluginManager().getPlugin("Worldme-Guild")} 获取实例后调用 getApi()。
 */
public interface GuildApi {

    Guild guildOf(UUID player);

    Guild guildById(int id);

    Guild guildByName(String name);

    List<Guild> all();

    /**
     * 公会领地 id（Worldme-Territory 中的领地）。
     */
    int regionIdOf(Guild guild);

    boolean deposit(UUID player, double amount);

    boolean withdraw(Player player, double amount);

    void notify(UUID player, String miniMessage);

    /**
     * 注册公会功能，供本模块或外部模块扩展。
     */
    void registerFeature(GuildFeatureProvider provider);

    FeatureRegistry featureRegistry();
}
