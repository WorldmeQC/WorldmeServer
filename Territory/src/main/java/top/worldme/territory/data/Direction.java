package top.worldme.territory.data;

/**
 * 扩展方向。水平方向以区块为单位，垂直方向以 16 格段为单位。
 */
public enum Direction {

    NORTH(Axis.Z, true),
    SOUTH(Axis.Z, false),
    EAST(Axis.X, false),
    WEST(Axis.X, true),
    UP(Axis.Y, false),
    DOWN(Axis.Y, true);

    public enum Axis {
        X,
        Z,
        Y
    }

    private final Axis axis;
    private final boolean negative;

    Direction(Axis axis, boolean negative) {
        this.axis = axis;
        this.negative = negative;
    }

    public Axis axis() {
        return axis;
    }

    /**
     * 是否朝坐标减小的一侧扩展（西 / 北 / 下）。
     */
    public boolean negative() {
        return negative;
    }

    public boolean vertical() {
        return axis == Axis.Y;
    }

    /**
     * 解析方向名称，支持英文与中文简称。
     */
    public static Direction byName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim().toLowerCase();
        return switch (key) {
            case "n", "north", "北" -> NORTH;
            case "s", "south", "南" -> SOUTH;
            case "e", "east", "东" -> EAST;
            case "w", "west", "西" -> WEST;
            case "up", "u", "上" -> UP;
            case "down", "d", "下" -> DOWN;
            default -> null;
        };
    }
}
