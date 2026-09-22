package top.worldme.territory.data;

import java.util.UUID;

/**
 * 领地成员及其权限掩码。
 */
public class RegionMember {

    private final UUID uuid;
    private RegionRole role;
    private int permissions;

    public RegionMember(UUID uuid, RegionRole role, int permissions) {
        this.uuid = uuid;
        this.role = role;
        this.permissions = permissions;
    }

    public UUID uuid() {
        return uuid;
    }

    public RegionRole role() {
        return role;
    }

    public void setRole(RegionRole role) {
        this.role = role;
    }

    public int permissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public boolean has(PermissionFlag flag) {
        return (permissions & flag.bit()) != 0;
    }

    public void set(PermissionFlag flag, boolean value) {
        if (value) {
            permissions |= flag.bit();
        } else {
            permissions &= ~flag.bit();
        }
    }
}
