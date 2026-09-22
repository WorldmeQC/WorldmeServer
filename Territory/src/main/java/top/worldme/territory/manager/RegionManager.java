package top.worldme.territory.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.territory.api.TerritoryApi;
import top.worldme.territory.api.event.RegionAccessEvent;
import top.worldme.territory.config.TerritoryConfig;
import top.worldme.territory.data.Direction;
import top.worldme.territory.data.OwnerType;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.RegionMember;
import top.worldme.territory.data.RegionRole;
import top.worldme.territory.data.SettingFlag;
import top.worldme.territory.data.SubRegion;
import top.worldme.territory.data.TerritoryDatabase;
import top.worldme.territory.economy.VaultHook;
import top.worldme.territory.util.ChunkKey;
import top.worldme.territory.util.RegionGeometry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RegionManager implements TerritoryApi {

    public static final String ADMIN_PERMISSION = "worldme.territory.admin";

    private final JavaPlugin plugin;
    private final TerritoryConfig config;
    private final TerritoryDatabase database;
    private final VaultHook vault;

    private final List<Region> regions = new CopyOnWriteArrayList<>();
    private final Map<Long, Region> byId = new ConcurrentHashMap<>();
    private final Map<ChunkKey, List<Region>> index = new ConcurrentHashMap<>();

    private final List<SubRegion> subRegions = new CopyOnWriteArrayList<>();
    private final Map<Long, SubRegion> subById = new ConcurrentHashMap<>();
    private final Map<ChunkKey, List<SubRegion>> subIndex = new ConcurrentHashMap<>();

    public RegionManager(JavaPlugin plugin, TerritoryConfig config, TerritoryDatabase database, VaultHook vault) {
        this.plugin = plugin;
        this.config = config;
        this.database = database;
        this.vault = vault;
    }

    // ---------- 加载 / 索引 ----------

    public void load() {
        regions.clear();
        byId.clear();
        index.clear();
        subRegions.clear();
        subById.clear();
        subIndex.clear();

        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM regions")) {
            while (rs.next()) {
                Region region = new Region(
                        rs.getInt("id"),
                        OwnerType.byName(rs.getString("owner_type")),
                        UUID.fromString(rs.getString("owner_id")),
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getInt("min_chunk_x"),
                        rs.getInt("min_section_y"),
                        rs.getInt("min_chunk_z"),
                        rs.getInt("chunks_x"),
                        rs.getInt("sections_y"),
                        rs.getInt("chunks_z"),
                        rs.getInt("flags"),
                        rs.getLong("created_at")
                );
                region.setWarpRaw(
                        rs.getInt("has_warp") == 1,
                        rs.getDouble("warp_x"),
                        rs.getDouble("warp_y"),
                        rs.getDouble("warp_z"),
                        (float) rs.getDouble("warp_yaw"),
                        (float) rs.getDouble("warp_pitch")
                );
                region.setEnterMessage(rs.getString("enter_msg"));
                region.setLeaveMessage(rs.getString("leave_msg"));
                regions.add(region);
                byId.put((long) region.id(), region);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载领地失败: " + e.getMessage());
        }

        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT region_id, player_uuid, role, permissions FROM region_members")) {
            while (rs.next()) {
                Region region = byId.get((long) rs.getInt("region_id"));
                if (region == null) {
                    continue;
                }
                region.addMember(new RegionMember(
                        UUID.fromString(rs.getString("player_uuid")),
                        RegionRole.byName(rs.getString("role")),
                        rs.getInt("permissions")
                ));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载领地成员失败: " + e.getMessage());
        }

        for (Region region : regions) {
            addToIndex(region);
        }

        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM sub_regions")) {
            while (rs.next()) {
                Region parent = byId.get((long) rs.getInt("region_id"));
                if (parent == null) {
                    continue;
                }
                SubRegion sub = new SubRegion(
                        rs.getInt("id"),
                        rs.getInt("region_id"),
                        rs.getString("name"),
                        rs.getInt("min_offset_x"),
                        rs.getInt("min_offset_y"),
                        rs.getInt("min_offset_z"),
                        rs.getInt("chunks_x"),
                        rs.getInt("sections_y"),
                        rs.getInt("chunks_z"),
                        rs.getInt("flags"),
                        rs.getLong("created_at")
                );
                sub.setParent(parent);
                subRegions.add(sub);
                subById.put((long) sub.id(), sub);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载子区域失败: " + e.getMessage());
        }

        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT sub_region_id, player_uuid, role, permissions FROM sub_region_members")) {
            while (rs.next()) {
                SubRegion sub = subById.get((long) rs.getInt("sub_region_id"));
                if (sub == null) {
                    continue;
                }
                sub.addMember(new RegionMember(
                        UUID.fromString(rs.getString("player_uuid")),
                        RegionRole.byName(rs.getString("role")),
                        rs.getInt("permissions")
                ));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载子区域成员失败: " + e.getMessage());
        }

        for (SubRegion sub : subRegions) {
            addToIndex(sub);
        }
        plugin.getLogger().info("领地加载完成：" + regions.size() + " 块，子区域 " + subRegions.size() + " 个。");
    }

    private void addToIndex(Region region) {
        for (int x = region.minChunkX(); x < region.maxChunkXExclusive(); x++) {
            for (int z = region.minChunkZ(); z < region.maxChunkZExclusive(); z++) {
                ChunkKey key = new ChunkKey(region.world(), x, z);
                List<Region> list = index.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
                if (!list.contains(region)) {
                    list.add(region);
                }
            }
        }
    }

    private void removeFromIndex(Region region) {
        for (int x = region.minChunkX(); x < region.maxChunkXExclusive(); x++) {
            for (int z = region.minChunkZ(); z < region.maxChunkZExclusive(); z++) {
                ChunkKey key = new ChunkKey(region.world(), x, z);
                List<Region> list = index.get(key);
                if (list == null) {
                    continue;
                }
                list.remove(region);
                if (list.isEmpty()) {
                    index.remove(key);
                }
            }
        }
    }

    private void addToIndex(SubRegion sub) {
        for (int x = sub.absoluteMinChunkX(); x < sub.absoluteMinChunkX() + sub.chunksX(); x++) {
            for (int z = sub.absoluteMinChunkZ(); z < sub.absoluteMinChunkZ() + sub.chunksZ(); z++) {
                ChunkKey key = new ChunkKey(sub.parent().world(), x, z);
                List<SubRegion> list = subIndex.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
                if (!list.contains(sub)) {
                    list.add(sub);
                }
            }
        }
    }

    private void removeFromIndex(SubRegion sub) {
        for (int x = sub.absoluteMinChunkX(); x < sub.absoluteMinChunkX() + sub.chunksX(); x++) {
            for (int z = sub.absoluteMinChunkZ(); z < sub.absoluteMinChunkZ() + sub.chunksZ(); z++) {
                ChunkKey key = new ChunkKey(sub.parent().world(), x, z);
                List<SubRegion> list = subIndex.get(key);
                if (list == null) {
                    continue;
                }
                list.remove(sub);
                if (list.isEmpty()) {
                    subIndex.remove(key);
                }
            }
        }
    }

    // ---------- 查询 ----------

    public Region getRegionAt(Location location) {        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        ChunkKey key = new ChunkKey(world.getName(),
                RegionGeometry.chunkOf(location.getBlockX()),
                RegionGeometry.chunkOf(location.getBlockZ()));
        List<Region> list = index.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (Region region : list) {
            if (region.contains(location)) {
                return region;
            }
        }
        return null;
    }

    /**
     * 查询位置所在子区域，多个重叠时取体积最小（最深层）的一个。
     */
    public SubRegion getSubRegionAt(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        ChunkKey key = new ChunkKey(world.getName(),
                RegionGeometry.chunkOf(location.getBlockX()),
                RegionGeometry.chunkOf(location.getBlockZ()));
        List<SubRegion> list = subIndex.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        SubRegion deepest = null;
        for (SubRegion sub : list) {
            if (!sub.contains(location)) {
                continue;
            }
            if (deepest == null || sub.volumeUnits() < deepest.volumeUnits()) {
                deepest = sub;
            }
        }
        return deepest;
    }

    @Override
    public Region regionAt(Location location) {
        return getRegionAt(location);
    }

    @Override
    public Region regionById(int id) {
        return getById(id);
    }

    @Override
    public SubRegion subRegionAt(Location location) {
        return getSubRegionAt(location);
    }

    @Override
    public boolean hasSetting(Location location, SettingFlag flag) {
        Region region = getRegionAt(location);
        if (region == null) {
            return true;
        }
        SubRegion sub = getSubRegionAt(location);
        return sub != null ? sub.hasSetting(flag) : region.hasSetting(flag);
    }

    @Override
    public boolean canInteract(Player player, Location location, PermissionFlag flag) {
        Region region = getRegionAt(location);
        if (region == null) {
            return true;
        }
        SubRegion sub = getSubRegionAt(location);
        boolean allowed = baseAllowed(player, region, sub, flag);
        RegionAccessEvent event = new RegionAccessEvent(player, region, sub, location, flag, allowed);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private boolean baseAllowed(Player player, Region region, SubRegion sub, PermissionFlag flag) {
        if (player.hasPermission(ADMIN_PERMISSION) || region.ownerId().equals(player.getUniqueId())) {
            return true;
        }
        if (sub != null) {
            RegionMember member = sub.member(player.getUniqueId());
            return member != null && member.has(flag);
        }
        return (region.memberPermissions(player.getUniqueId()) & flag.bit()) != 0;
    }

    @Override
    public List<Region> regionsOf(OwnerType type, UUID ownerId) {
        return getByOwner(type, ownerId);
    }

    @Override
    public int countOwned(OwnerType type, UUID ownerId) {
        return countByOwner(type, ownerId);
    }

    public Region getById(int id) {
        return byId.get((long) id);
    }

    public List<Region> all() {
        return new ArrayList<>(regions);
    }

    public List<Region> getByOwner(OwnerType type, UUID ownerId) {
        List<Region> result = new ArrayList<>();
        for (Region region : regions) {
            if (region.ownerType() == type && region.ownerId().equals(ownerId)) {
                result.add(region);
            }
        }
        return result;
    }

    public int countByOwner(OwnerType type, UUID ownerId) {
        int count = 0;
        for (Region region : regions) {
            if (region.ownerType() == type && region.ownerId().equals(ownerId)) {
                count++;
            }
        }
        return count;
    }

    public int maxClaims(Player player) {
        return config.maxClaims(player);
    }

    public TerritoryConfig config() {
        return config;
    }

    public VaultHook vault() {
        return vault;
    }

    /**
     * 按名称查找玩家拥有的领地（大小写不敏感）。
     */
    public Region findOwnedByName(Player player, String name) {
        for (Region region : getByOwner(OwnerType.PLAYER, player.getUniqueId())) {
            if (region.name().equalsIgnoreCase(name)) {
                return region;
            }
        }
        return null;
    }

    /**
     * 按名称查找任意领地（管理员/公会场景）。
     */
    public Region findByName(String name) {
        for (Region region : regions) {
            if (region.name().equalsIgnoreCase(name)) {
                return region;
            }
        }
        return null;
    }

    public SubRegion findSubRegion(Region region, String name) {
        for (SubRegion sub : subRegionsOf(region)) {
            if (sub.name().equalsIgnoreCase(name)) {
                return sub;
            }
        }
        return null;
    }

    // ---------- 创建 / 扩展 / 删除 ----------

    public RegionResult create(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null || !config.isWorldAllowed(world)) {
            return RegionResult.WORLD_DENIED;
        }
        int chunkX = RegionGeometry.chunkOf(location.getBlockX());
        int chunkZ = RegionGeometry.chunkOf(location.getBlockZ());
        int section = RegionGeometry.sectionOf(location.getBlockY());
        if (!withinWorldHeight(world, section, 1)) {
            return RegionResult.WORLD_BOUND;
        }
        UUID owner = player.getUniqueId();
        if (countByOwner(OwnerType.PLAYER, owner) >= config.maxClaims(player)) {
            return RegionResult.AT_CAP;
        }
        if (overlaps(world.getName(), chunkX, section, chunkZ, 1, 1, 1, -1)) {
            return RegionResult.OVERLAP;
        }

        double cost = config.costForUnits(1);
        if (cost > 0) {
            if (!vault.isReady()) {
                return RegionResult.ECONOMY;
            }
            if (!vault.withdraw(owner, cost)) {
                return RegionResult.NO_MONEY;
            }
        }

        String name = "领地" + (countByOwner(OwnerType.PLAYER, owner) + 1);
        Region region = insertRegion(OwnerType.PLAYER, owner, name, world.getName(),
                chunkX, section, chunkZ, 1, 1, 1, config.defaultSettingMask());
        if (region == null) {
            if (cost > 0) {
                vault.deposit(owner, cost);
            }
            return RegionResult.ERROR;
        }
        return RegionResult.OK;
    }

    private Region insertRegion(OwnerType ownerType, UUID ownerId, String name, String world,
                                int minChunkX, int minSectionY, int minChunkZ,
                                int chunksX, int sectionsY, int chunksZ, int flags) {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO regions(owner_type, owner_id, name, world, min_chunk_x, min_section_y, min_chunk_z,"
                        + " chunks_x, sections_y, chunks_z, flags, created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ownerType.name());
            ps.setString(2, ownerId.toString());
            ps.setString(3, name);
            ps.setString(4, world);
            ps.setInt(5, minChunkX);
            ps.setInt(6, minSectionY);
            ps.setInt(7, minChunkZ);
            ps.setInt(8, chunksX);
            ps.setInt(9, sectionsY);
            ps.setInt(10, chunksZ);
            ps.setInt(11, flags);
            ps.setLong(12, now);
            ps.executeUpdate();

            int id = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }
            Region region = new Region(id, ownerType, ownerId, name, world,
                    minChunkX, minSectionY, minChunkZ, chunksX, sectionsY, chunksZ, flags, now);
            regions.add(region);
            byId.put((long) id, region);
            addToIndex(region);
            return region;
        } catch (Exception e) {
            plugin.getLogger().warning("创建领地失败: " + e.getMessage());
            return null;
        }
    }

    public RegionResult expand(Player player, Region region, Direction direction, int units) {
        if (units <= 0) {
            return RegionResult.INVALID;
        }
        boolean owner = region.ownerId().equals(player.getUniqueId());
        if (!owner && !player.hasPermission(ADMIN_PERMISSION)) {
            return RegionResult.NOT_OWNER;
        }
        return applyExpand(region, direction, units, owner ? player.getUniqueId() : null, true);
    }

    @Override
    public boolean expandRegion(Region region, Direction direction, int units) {
        return units > 0 && applyExpand(region, direction, units, null, false) == RegionResult.OK;
    }

    private RegionResult applyExpand(Region region, Direction direction, int units, UUID payer, boolean enforceLimits) {
        int[] bounds = expandedBounds(region, direction, units);
        int minChunkX = bounds[0];
        int minSectionY = bounds[1];
        int minChunkZ = bounds[2];
        int chunksX = bounds[3];
        int sectionsY = bounds[4];
        int chunksZ = bounds[5];

        if (enforceLimits) {
            if (chunksX > config.maxHorizontalChunks() || chunksZ > config.maxHorizontalChunks()) {
                return RegionResult.LIMIT;
            }
            if (config.maxVerticalSections() > 0 && sectionsY > config.maxVerticalSections()) {
                return RegionResult.LIMIT;
            }
        }
        World world = Bukkit.getWorld(region.world());
        if (world == null) {
            return RegionResult.WORLD_DENIED;
        }
        if (!withinWorldHeight(world, minSectionY, sectionsY)) {
            return RegionResult.WORLD_BOUND;
        }
        if (overlaps(region.world(), minChunkX, minSectionY, minChunkZ, chunksX, sectionsY, chunksZ, region.id())) {
            return RegionResult.OVERLAP;
        }

        if (payer != null) {
            double cost = config.costForUnits(chunksX * sectionsY * chunksZ) - config.costForUnits(region.volumeUnits());
            if (cost > 0) {
                if (!vault.isReady()) {
                    return RegionResult.ECONOMY;
                }
                if (!vault.withdraw(payer, cost)) {
                    return RegionResult.NO_MONEY;
                }
            }
        }

        removeFromIndex(region);
        region.setBounds(minChunkX, minSectionY, minChunkZ, chunksX, sectionsY, chunksZ);
        updateBounds(region);
        addToIndex(region);
        return RegionResult.OK;
    }

    /**
     * 计算按方向扩展后的范围 {minChunkX, minSectionY, minChunkZ, chunksX, sectionsY, chunksZ}。
     */
    private int[] expandedBounds(Region region, Direction direction, int units) {
        int minChunkX = region.minChunkX();
        int minSectionY = region.minSectionY();
        int minChunkZ = region.minChunkZ();
        int chunksX = region.chunksX();
        int sectionsY = region.sectionsY();
        int chunksZ = region.chunksZ();

        switch (direction) {
            case EAST -> chunksX += units;
            case WEST -> {
                minChunkX -= units;
                chunksX += units;
            }
            case SOUTH -> chunksZ += units;
            case NORTH -> {
                minChunkZ -= units;
                chunksZ += units;
            }
            case UP -> sectionsY += units;
            case DOWN -> {
                minSectionY -= units;
                sectionsY += units;
            }
        }
        return new int[]{minChunkX, minSectionY, minChunkZ, chunksX, sectionsY, chunksZ};
    }

    /**
     * 计算按方向扩展指定单位数所需的费用（体积差价，可能为 0）。
     */
    public double expandCost(Region region, Direction direction, int units) {
        if (units <= 0) {
            return 0;
        }
        int[] bounds = expandedBounds(region, direction, units);
        int newVolume = bounds[3] * bounds[4] * bounds[5];
        return config.costForUnits(newVolume) - config.costForUnits(region.volumeUnits());
    }

    public RegionResult unclaim(Player player, Region region) {
        if (!region.ownerId().equals(player.getUniqueId()) && !player.hasPermission(ADMIN_PERMISSION)) {
            return RegionResult.NOT_OWNER;
        }
        removeRegionInternal(region);
        return RegionResult.OK;
    }

    @Override
    public Region createRegion(OwnerType type, UUID ownerId, String name, World world,
                               int minChunkX, int minSectionY, int minChunkZ,
                               int chunksX, int sectionsY, int chunksZ) {
        if (world == null || !config.isWorldAllowed(world)) {
            return null;
        }
        if (chunksX < 1 || sectionsY < 1 || chunksZ < 1) {
            return null;
        }
        if (!withinWorldHeight(world, minSectionY, sectionsY)) {
            return null;
        }
        if (overlaps(world.getName(), minChunkX, minSectionY, minChunkZ, chunksX, sectionsY, chunksZ, -1)) {
            return null;
        }
        return insertRegion(type, ownerId, name, world.getName(),
                minChunkX, minSectionY, minChunkZ, chunksX, sectionsY, chunksZ, config.defaultSettingMask());
    }

    @Override
    public boolean deleteRegion(Region region) {
        if (region == null || !regions.contains(region)) {
            return false;
        }
        removeRegionInternal(region);
        return true;
    }

    private void removeRegionInternal(Region region) {
        for (SubRegion sub : new ArrayList<>(subRegionsOf(region))) {
            deleteSubRegion(sub);
        }
        removeFromIndex(region);
        regions.remove(region);
        byId.remove((long) region.id());
        exec("DELETE FROM regions WHERE id=" + region.id());
        exec("DELETE FROM region_members WHERE region_id=" + region.id());
    }

    // ---------- 子区域 ----------

    @Override
    public boolean addRegionMember(Region region, UUID playerId, int permissions) {
        return addMember(region, playerId, permissions) == RegionResult.OK;
    }

    @Override
    public boolean removeRegionMember(Region region, UUID playerId) {
        return removeMember(region, playerId) == RegionResult.OK;
    }

    @Override
    public boolean hasRegionMember(Region region, UUID playerId) {
        return region != null && region.member(playerId) != null;
    }

    @Override
    public void setRegionMemberPermission(Region region, UUID playerId, PermissionFlag flag, boolean value) {
        setMemberPermission(region, playerId, flag, value);
    }

    @Override
    public void setRegionMemberPermissions(Region region, UUID playerId, int permissions) {
        if (region == null) {
            return;
        }
        RegionMember member = region.member(playerId);
        if (member == null) {
            return;
        }
        member.setPermissions(permissions);
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE region_members SET permissions=? WHERE region_id=? AND player_uuid=?")) {
            ps.setInt(1, permissions);
            ps.setInt(2, region.id());
            ps.setString(3, playerId.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新成员权限失败: " + e.getMessage());
        }
    }

    @Override
    public List<SubRegion> subRegionsOf(Region region) {
        List<SubRegion> result = new ArrayList<>();
        if (region == null) {
            return result;
        }
        for (SubRegion sub : subRegions) {
            if (sub.regionId() == region.id()) {
                result.add(sub);
            }
        }
        return result;
    }

    public SubRegion getSubRegionById(int id) {
        return subById.get((long) id);
    }

    @Override
    public SubRegion createSubRegion(Region parent, String name,
                                     int minOffsetX, int minOffsetY, int minOffsetZ,
                                     int chunksX, int sectionsY, int chunksZ) {
        if (parent == null || chunksX < 1 || sectionsY < 1 || chunksZ < 1) {
            return null;
        }
        if (minOffsetX < 0 || minOffsetY < 0 || minOffsetZ < 0) {
            return null;
        }
        if (minOffsetX + chunksX > parent.chunksX()
                || minOffsetY + sectionsY > parent.sectionsY()
                || minOffsetZ + chunksZ > parent.chunksZ()) {
            return null;
        }
        if (subRegionOverlaps(parent, minOffsetX, minOffsetY, minOffsetZ, chunksX, sectionsY, chunksZ, -1)) {
            return null;
        }
        return insertSubRegion(parent, name, minOffsetX, minOffsetY, minOffsetZ,
                chunksX, sectionsY, chunksZ, config.defaultSettingMask());
    }

    @Override
    public boolean deleteSubRegion(SubRegion sub) {
        if (sub == null || !subRegions.contains(sub)) {
            return false;
        }
        removeFromIndex(sub);
        subRegions.remove(sub);
        subById.remove((long) sub.id());
        exec("DELETE FROM sub_regions WHERE id=" + sub.id());
        exec("DELETE FROM sub_region_members WHERE sub_region_id=" + sub.id());
        return true;
    }

    private boolean subRegionOverlaps(Region parent, int offsetX, int offsetY, int offsetZ,
                                      int chunksX, int sectionsY, int chunksZ, int excludeId) {
        int minX = parent.minChunkX() + offsetX;
        int minY = parent.minSectionY() + offsetY;
        int minZ = parent.minChunkZ() + offsetZ;
        for (SubRegion sub : subRegions) {
            if (sub.regionId() != parent.id()) {
                continue;
            }
            if (excludeId >= 0 && sub.id() == excludeId) {
                continue;
            }
            int subMinX = sub.absoluteMinChunkX();
            int subMinY = sub.absoluteMinSectionY();
            int subMinZ = sub.absoluteMinChunkZ();
            if (subMinX < minX + chunksX && subMinX + sub.chunksX() > minX
                    && subMinZ < minZ + chunksZ && subMinZ + sub.chunksZ() > minZ
                    && subMinY < minY + sectionsY && subMinY + sub.sectionsY() > minY) {
                return true;
            }
        }
        return false;
    }

    private SubRegion insertSubRegion(Region parent, String name,
                                      int offsetX, int offsetY, int offsetZ,
                                      int chunksX, int sectionsY, int chunksZ, int flags) {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO sub_regions(region_id, name, min_offset_x, min_offset_y, min_offset_z,"
                        + " chunks_x, sections_y, chunks_z, flags, created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, parent.id());
            ps.setString(2, name);
            ps.setInt(3, offsetX);
            ps.setInt(4, offsetY);
            ps.setInt(5, offsetZ);
            ps.setInt(6, chunksX);
            ps.setInt(7, sectionsY);
            ps.setInt(8, chunksZ);
            ps.setInt(9, flags);
            ps.setLong(10, now);
            ps.executeUpdate();

            int id = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }
            SubRegion sub = new SubRegion(id, parent.id(), name, offsetX, offsetY, offsetZ,
                    chunksX, sectionsY, chunksZ, flags, now);
            sub.setParent(parent);
            subRegions.add(sub);
            subById.put((long) id, sub);
            addToIndex(sub);
            return sub;
        } catch (Exception e) {
            plugin.getLogger().warning("创建子区域失败: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean addSubRegionMember(SubRegion subRegion, UUID playerId, int permissions) {
        if (subRegion == null || subRegion.member(playerId) != null) {
            return false;
        }
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO sub_region_members(sub_region_id, player_uuid, role, permissions) VALUES(?,?,?,?)")) {
            ps.setInt(1, subRegion.id());
            ps.setString(2, playerId.toString());
            ps.setString(3, RegionRole.MEMBER.name());
            ps.setInt(4, permissions);
            ps.executeUpdate();
            subRegion.addMember(new RegionMember(playerId, RegionRole.MEMBER, permissions));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("添加子区域成员失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeSubRegionMember(SubRegion subRegion, UUID playerId) {
        if (subRegion == null || subRegion.member(playerId) == null) {
            return false;
        }
        exec("DELETE FROM sub_region_members WHERE sub_region_id=" + subRegion.id()
                + " AND player_uuid='" + playerId + "'");
        subRegion.removeMember(playerId);
        return true;
    }

    @Override
    public void setSubRegionMemberPermission(SubRegion subRegion, UUID playerId, PermissionFlag flag, boolean value) {
        if (subRegion == null) {
            return;
        }
        RegionMember member = subRegion.member(playerId);
        if (member == null) {
            return;
        }
        member.set(flag, value);
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE sub_region_members SET permissions=? WHERE sub_region_id=? AND player_uuid=?")) {
            ps.setInt(1, member.permissions());
            ps.setInt(2, subRegion.id());
            ps.setString(3, playerId.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新子区域成员权限失败: " + e.getMessage());
        }
    }

    public void setSubRegionSetting(SubRegion sub, SettingFlag flag, boolean value) {
        sub.setSetting(flag, value);
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE sub_regions SET flags=? WHERE id=?")) {
            ps.setInt(1, sub.flags());
            ps.setInt(2, sub.id());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新子区域设置失败: " + e.getMessage());
        }
    }


    // ---------- 修改 ----------

    public void rename(Region region, String name) {
        region.setName(name);
        updateText("UPDATE regions SET name=? WHERE id=" + region.id(), name);
    }

    public void setEnterMessage(Region region, String message) {
        region.setEnterMessage(message);
        updateText("UPDATE regions SET enter_msg=? WHERE id=" + region.id(), region.enterMessage());
    }

    public void setLeaveMessage(Region region, String message) {
        region.setLeaveMessage(message);
        updateText("UPDATE regions SET leave_msg=? WHERE id=" + region.id(), region.leaveMessage());
    }

    public void setHome(Region region, Location location) {
        region.setWarp(location);
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE regions SET has_warp=1, warp_x=?, warp_y=?, warp_z=?, warp_yaw=?, warp_pitch=? WHERE id=?")) {
            ps.setDouble(1, location.getX());
            ps.setDouble(2, location.getY());
            ps.setDouble(3, location.getZ());
            ps.setDouble(4, location.getYaw());
            ps.setDouble(5, location.getPitch());
            ps.setInt(6, region.id());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("设置领地传送点失败: " + e.getMessage());
        }
    }

    public void setSetting(Region region, SettingFlag flag, boolean value) {
        region.setSetting(flag, value);
        updateFlags(region);
    }

    private void updateBounds(Region region) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE regions SET min_chunk_x=?, min_section_y=?, min_chunk_z=?, chunks_x=?, sections_y=?, chunks_z=? WHERE id=?")) {
            ps.setInt(1, region.minChunkX());
            ps.setInt(2, region.minSectionY());
            ps.setInt(3, region.minChunkZ());
            ps.setInt(4, region.chunksX());
            ps.setInt(5, region.sectionsY());
            ps.setInt(6, region.chunksZ());
            ps.setInt(7, region.id());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新领地范围失败: " + e.getMessage());
        }
    }

    private void updateFlags(Region region) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE regions SET flags=? WHERE id=?")) {
            ps.setInt(1, region.flags());
            ps.setInt(2, region.id());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新领地设置失败: " + e.getMessage());
        }
    }

    private void updateText(String sql, String value) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, value);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新领地文本失败: " + e.getMessage());
        }
    }

    private void exec(String sql) {
        try (Statement st = database.getConnection().createStatement()) {
            st.executeUpdate(sql);
        } catch (Exception e) {
            plugin.getLogger().warning("领地 SQL 失败: " + e.getMessage());
        }
    }

    // ---------- 校验工具 ----------

    public boolean overlaps(String world, int minChunkX, int minSectionY, int minChunkZ,
                            int chunksX, int sectionsY, int chunksZ, int excludeId) {
        for (Region region : regions) {
            if (excludeId >= 0 && region.id() == excludeId) {
                continue;
            }
            if (region.intersectsBounds(world, minChunkX, minSectionY, minChunkZ, chunksX, sectionsY, chunksZ)) {
                return true;
            }
        }
        return false;
    }

    public boolean withinWorldHeight(World world, int minSection, int sections) {
        int worldMin = RegionGeometry.sectionOf(world.getMinHeight());
        int worldMax = RegionGeometry.sectionOf(world.getMaxHeight() - 1);
        return minSection >= worldMin && minSection + sections - 1 <= worldMax;
    }

    /**
     * 玩家是否为领地主人或管理员。
     */
    public boolean isOwnerOrAdmin(Player player, Region region) {
        return region.ownerId().equals(player.getUniqueId()) || player.hasPermission(ADMIN_PERMISSION);
    }

    /**
     * 玩家是否拥有领地内指定权限（主人拥有全部权限，管理员绕过）。
     */
    public boolean canInteract(Player player, Region region, PermissionFlag flag) {
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        return (region.memberPermissions(player.getUniqueId()) & flag.bit()) != 0;
    }

    // ---------- 成员管理 ----------

    public RegionResult addMember(Region region, UUID uuid, int permissions) {
        if (region.ownerId().equals(uuid)) {
            return RegionResult.MEMBER_IS_OWNER;
        }
        if (region.member(uuid) != null) {
            return RegionResult.MEMBER_EXISTS;
        }
        if (config.maxMembers() > 0 && region.members().size() >= config.maxMembers()) {
            return RegionResult.MEMBER_LIMIT;
        }
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO region_members(region_id, player_uuid, role, permissions) VALUES(?,?,?,?)")) {
            ps.setInt(1, region.id());
            ps.setString(2, uuid.toString());
            ps.setString(3, RegionRole.MEMBER.name());
            ps.setInt(4, permissions);
            ps.executeUpdate();
            region.addMember(new RegionMember(uuid, RegionRole.MEMBER, permissions));
            return RegionResult.OK;
        } catch (Exception e) {
            plugin.getLogger().warning("添加领地成员失败: " + e.getMessage());
            return RegionResult.ERROR;
        }
    }

    public RegionResult removeMember(Region region, UUID uuid) {
        if (region.member(uuid) == null) {
            return RegionResult.NOT_FOUND;
        }
        exec("DELETE FROM region_members WHERE region_id=" + region.id() + " AND player_uuid='" + uuid + "'");
        region.removeMember(uuid);
        return RegionResult.OK;
    }

    public void setMemberPermission(Region region, UUID uuid, PermissionFlag flag, boolean value) {
        RegionMember member = region.member(uuid);
        if (member == null) {
            return;
        }
        member.set(flag, value);
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE region_members SET permissions=? WHERE region_id=? AND player_uuid=?")) {
            ps.setInt(1, member.permissions());
            ps.setInt(2, region.id());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新成员权限失败: " + e.getMessage());
        }
    }

    public enum RegionResult {
        OK,
        AT_CAP,
        OVERLAP,
        WORLD_DENIED,
        WORLD_BOUND,
        LIMIT,
        NO_MONEY,
        ECONOMY,
        NOT_OWNER,
        NOT_FOUND,
        MEMBER_EXISTS,
        MEMBER_IS_OWNER,
        MEMBER_LIMIT,
        INVALID,
        ERROR
    }
}
