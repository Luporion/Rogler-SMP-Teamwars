package dev.rogler.teamwars.core.base;

import java.util.Collection;
import java.util.UUID;

/**
 * Event-local, dimension-independent projection of registered core protection zones.
 * Build a new instance for each explosion from authoritative server ledger entries:
 * no stale global cache, no Minecraft lookups, no force-loading of world chunks.
 */
public final class CoreExplosionIndex {
    public record Center(UUID team, int blockX, int blockZ) {}
    // Small primitive open-address table. Avoids allocating one boxed Long per
    // affected block when Warium passes hundreds of thousands of positions.
    private final long[] keys;
    private final boolean[] occupied;
    private int count;

    private CoreExplosionIndex(int expected) {
        int capacity = 16;
        while (capacity < Math.max(1L, (long) expected * 2)) capacity <<= 1;
        this.keys = new long[capacity];
        this.occupied = new boolean[capacity];
    }

    public static CoreExplosionIndex build(Collection<Center> centers, int chunkRadius) {
        if (chunkRadius < 0 || chunkRadius > 3) throw new IllegalArgumentException("chunk radius 0..3");
        int diameter = chunkRadius * 2 + 1;
        CoreExplosionIndex result = new CoreExplosionIndex(Math.multiplyExact(centers.size(), diameter * diameter));
        for (Center center : centers) {
            if (center.team() == null) throw new IllegalArgumentException("missing team UUID");
            int x = center.blockX() >> 4, z = center.blockZ() >> 4;
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                long cx = (long)x + dx;
                if (cx < Integer.MIN_VALUE || cx > Integer.MAX_VALUE) continue;
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    long cz = (long)z + dz;
                    if (cz < Integer.MIN_VALUE || cz > Integer.MAX_VALUE) continue;
                    result.add(key((int)cx, (int)cz));
                }
            }
        }
        return result;
    }

    public boolean protectsBlock(int blockX, int blockZ) {
        long target = key(blockX >> 4, blockZ >> 4);
        int slot = hash(target) & (keys.length - 1);
        while (occupied[slot]) {
            if (keys[slot] == target) return true;
            slot = (slot + 1) & (keys.length - 1);
        }
        return false;
    }

    public int coveredChunks() { return count; }

    private void add(long target) {
        int slot = hash(target) & (keys.length - 1);
        while (occupied[slot]) {
            if (keys[slot] == target) return;
            slot = (slot + 1) & (keys.length - 1);
        }
        occupied[slot] = true;
        keys[slot] = target;
        count++;
    }

    private static int hash(long key) {
        int h = (int)(key ^ (key >>> 32));
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        h *= 0x846ca68b;
        return h ^ (h >>> 16);
    }

    private static long key(int x, int z) {
        return ((long)x << 32) | (z & 0xffffffffL);
    }
}
