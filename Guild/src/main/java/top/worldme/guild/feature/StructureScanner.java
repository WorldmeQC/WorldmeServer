package top.worldme.guild.feature;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import top.worldme.guild.config.StructureConfig;
import top.worldme.guild.config.StructurePattern;
import top.worldme.territory.data.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构识别器：在公会领地范围内匹配 structures.yml 定义的结构图案。
 */
public final class StructureScanner {

    private StructureScanner() {
    }

    /**
     * 扫描整个领地，返回匹配到的结构键。
     */
    public static List<String> scan(Region region, StructureConfig config) {
        List<String> matched = new ArrayList<>();
        World world = Bukkit.getWorld(region.world());
        if (world == null) {
            return matched;
        }
        int minX = region.minChunkX() << 4;
        int minZ = region.minChunkZ() << 4;
        int minY = region.minSectionY() << 4;
        int maxX = (region.minChunkX() + region.chunksX()) << 4;
        int maxZ = (region.minChunkZ() + region.chunksZ()) << 4;
        int maxY = (region.minSectionY() + region.sectionsY()) << 4;

        for (StructurePattern pattern : config.structures().values()) {
            if (pattern.requirements().isEmpty()) {
                continue;
            }
            boolean found = false;
            for (int y = minY; y < maxY && !found; y++) {
                for (int x = minX; x < maxX && !found; x++) {
                    for (int z = minZ; z < maxZ && !found; z++) {
                        if (matchesAt(world, pattern, x, y, z)) {
                            found = true;
                        }
                    }
                }
            }
            if (found) {
                matched.add(pattern.key());
            }
        }
        return matched;
    }

    /**
     * 以刚放置的方块为线索，判断其是否补全了某个结构。
     */
    public static List<String> checkAt(Region region, Location placed, StructureConfig config) {
        List<String> matched = new ArrayList<>();
        World world = placed.getWorld();
        if (world == null) {
            return matched;
        }
        int px = placed.getBlockX();
        int py = placed.getBlockY();
        int pz = placed.getBlockZ();
        for (StructurePattern pattern : config.structures().values()) {
            for (StructurePattern.BlockRequirement requirement : pattern.requirements()) {
                int anchorX = px - requirement.dx();
                int anchorY = py - requirement.dy();
                int anchorZ = pz - requirement.dz();
                if (matchesAt(world, pattern, anchorX, anchorY, anchorZ)) {
                    if (!matched.contains(pattern.key())) {
                        matched.add(pattern.key());
                    }
                    break;
                }
            }
        }
        return matched;
    }

    private static boolean matchesAt(World world, StructurePattern pattern, int anchorX, int anchorY, int anchorZ) {
        if (pattern.requirements().isEmpty()) {
            return false;
        }
        for (StructurePattern.BlockRequirement requirement : pattern.requirements()) {
            Block block = world.getBlockAt(anchorX + requirement.dx(), anchorY + requirement.dy(),
                    anchorZ + requirement.dz());
            if (block.getType() != requirement.material()) {
                return false;
            }
        }
        return true;
    }
}
