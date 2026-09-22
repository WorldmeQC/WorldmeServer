package top.worldme.territory.data;

/**
 * 领地归属类型：个人玩家或公会。
 */
public enum OwnerType {
    PLAYER,
    GUILD;

    public static OwnerType byName(String name) {
        if (name == null) {
            return PLAYER;
        }
        for (OwnerType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return PLAYER;
    }
}
