package top.worldme.territory.data;

import org.bukkit.Location;
import org.bukkit.World;
import top.worldme.territory.util.RegionGeometry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 领地内的子区域。仅对公会开放，拥有独立的设置与成员权限。
 * 偏移与尺寸均以父领地的最小区块/段为原点，单位为区块（长宽）与 16 格段（高度）。
 */
public class SubRegion {

    private final int id;
    private final int regionId;
    private String name;
    private final int minOffsetX;
    private final int minOffsetY;
    private final int minOffsetZ;
    private final int chunksX;
    private final int sectionsY;
    private final int chunksZ;
    private int flags;
    private final long createdAt;
    private Region parent;
    private final Map<UUID, RegionMember> members = new HashMap<>();

    public SubRegion(int id, int regionId, String name,
                     int minOffsetX, int minOffsetY, int minOffsetZ,
                     int chunksX, int sectionsY, int chunksZ,
                     int flags, long createdAt) {
        this.id = id;
        this.regionId = regionId;
        this.name = name;
        this.minOffsetX = minOffsetX;
        this.minOffsetY = minOffsetY;
        this.minOffsetZ = minOffsetZ;
        this.chunksX = chunksX;
        this.sectionsY = sectionsY;
        this.chunksZ = chunksZ;
        this.flags = flags;
        this.createdAt = createdAt;
    }

    public int id() {
        return id;
    }

    public int regionId() {
        return regionId;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int minOffsetX() {
        return minOffsetX;
    }

    public int minOffsetY() {
        return minOffsetY;
    }

    public int minOffsetZ() {
        return minOffsetZ;
    }

    public int chunksX() {
        return chunksX;
    }

    public int sectionsY() {
        return sectionsY;
    }

    public int chunksZ() {
        return chunksZ;
    }

    public int flags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public long createdAt() {
        return createdAt;
    }

    public Region parent() {
        return parent;
    }

    public void setParent(Region parent) {
        this.parent = parent;
    }

    public int volumeUnits() {
        return chunksX * sectionsY * chunksZ;
    }

    public int absoluteMinChunkX() {
        return parent == null ? minOffsetX : parent.minChunkX() + minOffsetX;
    }

    public int absoluteMinSectionY() {
        return parent == null ? minOffsetY : parent.minSectionY() + minOffsetY;
    }

    public int absoluteMinChunkZ() {
        return parent == null ? minOffsetZ : parent.minChunkZ() + minOffsetZ;
    }

    public boolean hasSetting(SettingFlag flag) {
        return (flags & flag.bit()) != 0;
    }

    public void setSetting(SettingFlag flag, boolean value) {
        if (value) {
            flags |= flag.bit();
        } else {
            flags &= ~flag.bit();
        }
    }

    public Map<UUID, RegionMember> members() {
        return Collections.unmodifiableMap(members);
    }

    public Collection<RegionMember> memberList() {
        return Collections.unmodifiableCollection(members.values());
    }

    public RegionMember member(UUID uuid) {
        return members.get(uuid);
    }

    public void addMember(RegionMember member) {
        members.put(member.uuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean contains(Location location) {
        if (parent == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null || !world.getName().equals(parent.world())) {
            return false;
        }
        int chunkX = RegionGeometry.chunkOf(location.getBlockX());
        int chunkZ = RegionGeometry.chunkOf(location.getBlockZ());
        int section = RegionGeometry.sectionOf(location.getBlockY());
        int minX = parent.minChunkX() + minOffsetX;
        int minY = parent.minSectionY() + minOffsetY;
        int minZ = parent.minChunkZ() + minOffsetZ;
        return chunkX >= minX && chunkX < minX + chunksX
                && chunkZ >= minZ && chunkZ < minZ + chunksZ
                && section >= minY && section < minY + sectionsY;
    }

    public String boundsString() {
        int minX = absoluteMinChunkX();
        int minY = absoluteMinSectionY();
        int minZ = absoluteMinChunkZ();
        return minX + "," + minY + "," + minZ + " ～ "
                + (minX + chunksX - 1) + "," + (minY + sectionsY - 1) + "," + (minZ + chunksZ - 1);
    }
}
