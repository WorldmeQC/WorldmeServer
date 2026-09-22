package top.worldme.territory.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import top.worldme.territory.data.Direction;
import top.worldme.territory.data.OwnerType;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.SettingFlag;
import top.worldme.territory.data.SubRegion;

import java.util.List;
import java.util.UUID;

/**
 * 领地外部接口，供其他模块（如公会）调用。
 * 通过 {@code (Territory) Bukkit.getPluginManager().getPlugin("Worldme-Territory")} 获取实例后调用 getApi()。
 */
public interface TerritoryApi {

    /**
     * 查询位置所在领地，无则返回 null。
     */
    Region regionAt(Location location);

    /**
     * 按 id 查询领地。
     */
    Region regionById(int id);

    /**
     * 查询位置所在子区域（最深层），无则返回 null。
     */
    SubRegion subRegionAt(Location location);

    /**
     * 查询位置生效的设置（子区域优先于父领地），野外返回 true。
     */
    boolean hasSetting(Location location, SettingFlag flag);

    /**
     * 判断玩家在指定位置是否拥有某权限（含子区域与事件覆盖）。
     */
    boolean canInteract(Player player, Location location, PermissionFlag flag);

    List<Region> regionsOf(OwnerType type, UUID ownerId);

    int countOwned(OwnerType type, UUID ownerId);

    int maxClaims(Player player);

    /**
     * 以程序方式创建领地（不收费，供公会等系统使用）。
     * 重叠、世界限制或高度越界时返回 null。
     */
    Region createRegion(OwnerType type, UUID ownerId, String name, World world,
                        int minChunkX, int minSectionY, int minChunkZ,
                        int chunksX, int sectionsY, int chunksZ);

    /**
     * 以程序方式扩展领地（不收费）。返回是否成功。
     */
    boolean expandRegion(Region region, Direction direction, int units);

    /**
     * 删除领地及其全部子区域。
     */
    boolean deleteRegion(Region region);

    boolean addRegionMember(Region region, UUID playerId, int permissions);

    boolean removeRegionMember(Region region, UUID playerId);

    boolean hasRegionMember(Region region, UUID playerId);

    void setRegionMemberPermission(Region region, UUID playerId, PermissionFlag flag, boolean value);

    void setRegionMemberPermissions(Region region, UUID playerId, int permissions);

    List<SubRegion> subRegionsOf(Region region);

    /**
     * 在领地内创建子区域（仅公会系统使用）。越界或与已有子区域重叠时返回 null。
     */
    SubRegion createSubRegion(Region parent, String name,
                              int minOffsetX, int minOffsetY, int minOffsetZ,
                              int chunksX, int sectionsY, int chunksZ);

    boolean deleteSubRegion(SubRegion subRegion);

    boolean addSubRegionMember(SubRegion subRegion, UUID playerId, int permissions);

    boolean removeSubRegionMember(SubRegion subRegion, UUID playerId);

    void setSubRegionMemberPermission(SubRegion subRegion, UUID playerId, PermissionFlag flag, boolean value);
}
