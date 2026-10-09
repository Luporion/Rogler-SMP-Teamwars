package dev.rogler.teamwars.core;

import java.util.Objects;

/** World-coordinate chunk, never a client-owned map tile or a pre-generated chunk. */
public record ChunkKey(String dimension, int x, int z) implements Comparable<ChunkKey> {
    public ChunkKey {
        if (dimension == null || dimension.isBlank() || dimension.length() > 256)
            throw new IllegalArgumentException("Invalid dimension");
    }
    @Override public int compareTo(ChunkKey other) {
        int d = dimension.compareTo(other.dimension());
        if (d != 0) return d;
        int a = Integer.compare(x, other.x());
        return a != 0 ? a : Integer.compare(z, other.z());
    }
    public static ChunkKey fromBlock(String dimension, int blockX, int blockZ) {
        return new ChunkKey(dimension, Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
    }
}
