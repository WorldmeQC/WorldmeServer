package top.worldme.territory.data;

/**
 * 领地成员角色。
 */
public enum RegionRole {
    OWNER,
    MEMBER;

    public static RegionRole byName(String name) {
        if (name == null) {
            return MEMBER;
        }
        for (RegionRole role : values()) {
            if (role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return MEMBER;
    }
}
