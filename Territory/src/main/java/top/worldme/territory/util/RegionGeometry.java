package top.worldme.territory.util;

/**
 * 领地几何工具：区块与 16 格段的换算。
 */
public final class RegionGeometry {

    public static final int SECTION_HEIGHT = 16;

    private RegionGeometry() {
    }

    public static int chunkOf(int block) {
        return block >> 4;
    }

    public static int sectionOf(int blockY) {
        return Math.floorDiv(blockY, SECTION_HEIGHT);
    }

    public static int sectionMinY(int section) {
        return section * SECTION_HEIGHT;
    }
}
