package top.worldme.territory.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import top.worldme.territory.util.RegionGeometry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 一块领地。以 16×16×16 为单位：长宽按区块数、高度按 16 格段数记录。
 */
public class Region {

    private final int id;
    private final OwnerType ownerType;
    private final UUID ownerId;
    private String name;
    private final String world;
    private int minChunkX;
    private int minSectionY;
    private int minChunkZ;
    private int chunksX;
    private int sectionsY;
    private int chunksZ;
    private int flags;
    private boolean hasWarp;
    private double warpX;
    private double warpY;
    private double warpZ;
    private float warpYaw;
    private float warpPitch;
    private String enterMessage;
    private String leaveMessage;
    private final long createdAt;
    private final Map<UUID, RegionMember> members = new HashMap<>();

    public Region(int id, OwnerType ownerType, UUID ownerId, String name, String world,
                  int minChunkX, int minSectionY, int minChunkZ,
                  int chunksX, int sectionsY, int chunksZ,
                  int flags, long createdAt) {
        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.name = name;
        this.world = world;
        this.minChunkX = minChunkX;
        this.minSectionY = minSectionY;
        this.minChunkZ = minChunkZ;
        this.chunksX = chunksX;
        this.sectionsY = sectionsY;
        this.chunksZ = chunksZ;
        this.flags = flags;
        this.createdAt = createdAt;
    }

    public int id() {
        return id;
    }

    public OwnerType ownerType() {
        return ownerType;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String world() {
        return world;
    }

    public int minChunkX() {
        return minChunkX;
    }

    public int minSectionY() {
        return minSectionY;
    }

    public int minChunkZ() {
        return minChunkZ;
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

    public void setBounds(int minChunkX, int minSectionY, int minChunkZ,
                          int chunksX, int sectionsY, int chunksZ) {
        this.minChunkX = minChunkX;
        this.minSectionY = minSectionY;
        this.minChunkZ = minChunkZ;
        this.chunksX = chunksX;
        this.sectionsY = sectionsY;
        this.chunksZ = chunksZ;
    }

    public int maxChunkXExclusive() {
        return minChunkX + chunksX;
    }

    public int maxChunkZExclusive() {
        return minChunkZ + chunksZ;
    }

    public int maxSectionYExclusive() {
        return minSectionY + sectionsY;
    }

    public int flags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
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

    public boolean hasWarp() {
        return hasWarp;
    }

    public void setWarp(Location location) {
        this.hasWarp = true;
        this.warpX = location.getX();
        this.warpY = location.getY();
        this.warpZ = location.getZ();
        this.warpYaw = location.getYaw();
        this.warpPitch = location.getPitch();
    }

    public void setWarpRaw(boolean hasWarp, double x, double y, double z, float yaw, float pitch) {
        this.hasWarp = hasWarp;
        this.warpX = x;
        this.warpY = y;
        this.warpZ = z;
        this.warpYaw = yaw;
        this.warpPitch = pitch;
    }

    public double warpX() {
        return warpX;
    }

    public double warpY() {
        return warpY;
    }

    public double warpZ() {
        return warpZ;
    }

    public float warpYaw() {
        return warpYaw;
    }

    public float warpPitch() {
        return warpPitch;
    }

    public Location getWarp() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, warpX, warpY, warpZ, warpYaw, warpPitch);
    }

    public String enterMessage() {
        return enterMessage;
    }

    public void setEnterMessage(String enterMessage) {
        this.enterMessage = (enterMessage == null || enterMessage.isBlank()) ? null : enterMessage;
    }

    public String leaveMessage() {
        return leaveMessage;
    }

    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = (leaveMessage == null || leaveMessage.isBlank()) ? null : leaveMessage;
    }

    public long createdAt() {
        return createdAt;
    }

    public int volumeUnits() {
        return chunksX * sectionsY * chunksZ;
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

    public int memberPermissions(UUID uuid) {
        if (ownerId.equals(uuid)) {
            return PermissionFlag.all();
        }
        RegionMember member = members.get(uuid);
        if (member != null) {
            return member.permissions();
        }
        return 0;
    }

    public boolean contains(Location location) {
        World bukkitWorld = location.getWorld();
        if (bukkitWorld == null || !bukkitWorld.getName().equals(world)) {
            return false;
        }
        int chunkX = RegionGeometry.chunkOf(location.getBlockX());
        int chunkZ = RegionGeometry.chunkOf(location.getBlockZ());
        int section = RegionGeometry.sectionOf(location.getBlockY());
        return chunkX >= minChunkX && chunkX < maxChunkXExclusive()
                && chunkZ >= minChunkZ && chunkZ < maxChunkZExclusive()
                && section >= minSectionY && section < maxSectionYExclusive();
    }

    public boolean intersects(Region other) {
        return intersectsBounds(other.world, other.minChunkX, other.minSectionY, other.minChunkZ,
                other.chunksX, other.sectionsY, other.chunksZ);
    }

    /**
     * 判断给定范围（区块/段单位）是否与本领地相交。
     */
    public boolean intersectsBounds(String otherWorld, int minChunkX, int minSectionY, int minChunkZ,
                                    int chunksX, int sectionsY, int chunksZ) {
        if (!this.world.equals(otherWorld)) {
            return false;
        }
        return this.minChunkX < minChunkX + chunksX && this.maxChunkXExclusive() > minChunkX
                && this.minChunkZ < minChunkZ + chunksZ && this.maxChunkZExclusive() > minChunkZ
                && this.minSectionY < minSectionY + sectionsY && this.maxSectionYExclusive() > minSectionY;
    }

    /**
     * 领地范围的文本表示（区块坐标）。
     */
    public String boundsString() {
        return minChunkX + "," + minSectionY + "," + minChunkZ + " ～ "
                + (maxChunkXExclusive() - 1) + "," + (maxSectionYExclusive() - 1) + "," + (maxChunkZExclusive() - 1);
    }
}
