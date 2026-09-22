package top.worldme.territory.util;

/**
 * 空间索引使用的区块键：世界 + 区块 X/Z。
 */
public record ChunkKey(String world, int chunkX, int chunkZ) {

    public static ChunkKey of(String world, int blockX, int blockZ) {
        return new ChunkKey(world, blockX >> 4, blockZ >> 4);
    }
}
