package top.worldme.guild.feature;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import top.worldme.guild.data.Guild;

/**
 * 公会功能提供者。新增功能只需实现本接口并注册到 {@link FeatureRegistry}。
 */
public interface GuildFeatureProvider {

    /**
     * 功能唯一标识，与 structures.yml 中 features 的键一致。
     */
    String key();

    default String displayName() {
        return key();
    }

    default int requiredLevel() {
        return 1;
    }

    default UnlockType unlockType() {
        return UnlockType.STRUCTURE;
    }

    /**
     * 绑定的结构键；仅 STRUCTURE / BOTH 解锁方式需要。
     */
    default String structureKey() {
        return null;
    }

    /**
     * 结构匹配成功、首次解锁时调用。
     */
    default void onUnlock(Guild guild, Location location) {
    }

    default void onEnable(Guild guild) {
    }

    default void onDisable(Guild guild) {
    }

    /**
     * 定期效果（如信标 buff），由管理器按周期调用。
     */
    default void onTick(Guild guild) {
    }

    /**
     * 打开功能专属界面。返回是否已处理。
     */
    default boolean open(Guild guild, Player player) {
        return false;
    }
}
