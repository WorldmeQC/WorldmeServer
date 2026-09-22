package top.worldme.guild.config;

import org.bukkit.Material;

import java.util.List;

/**
 * 结构图案定义：扫描范围 + 相对方块要求。
 */
public class StructurePattern {

    private final String key;
    private final int boundX;
    private final int boundY;
    private final int boundZ;
    private final List<BlockRequirement> requirements;

    public StructurePattern(String key, int boundX, int boundY, int boundZ, List<BlockRequirement> requirements) {
        this.key = key;
        this.boundX = boundX;
        this.boundY = boundY;
        this.boundZ = boundZ;
        this.requirements = requirements;
    }

    public String key() {
        return key;
    }

    public int boundX() {
        return boundX;
    }

    public int boundY() {
        return boundY;
    }

    public int boundZ() {
        return boundZ;
    }

    public List<BlockRequirement> requirements() {
        return requirements;
    }

    public record BlockRequirement(int dx, int dy, int dz, Material material) {
    }
}
