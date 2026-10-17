package dev.rogler.teamwars.core;

import java.util.Objects;
import java.util.UUID;

/**
 * Samples real player travel only. Never considers pregenerated Chunky chunks discovered.
 * Bounded interpolation fills gaps between nearby samples, but does not reveal teleport paths.
 * Must be called from a single server thread.
 */
public final class ExplorationTrail {
    private final ExplorationIndex index;
    private final java.util.Map<UUID, ChunkKey> previous = new java.util.HashMap<>();
    private final int interpolationLimit;

    public ExplorationTrail(ExplorationIndex index, int interpolationLimit) {
        this.index = Objects.requireNonNull(index);
        if (interpolationLimit < 1 || interpolationLimit > 32) throw new IllegalArgumentException("interpolationLimit");
        this.interpolationLimit = interpolationLimit;
    }

    public int sample(UUID player, ChunkKey current) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(current);
        ChunkKey last = previous.put(player, current);
        int newChunks = 0;
        if (last != null && last.dimension().equals(current.dimension())) {
            long dx = Math.abs((long)last.x() - current.x());
            long dz = Math.abs((long)last.z() - current.z());
            if (Math.max(dx, dz) <= interpolationLimit) {
                int x = last.x(), z = last.z(), tx = current.x(), tz = current.z();
                int ax = (int)dx, az = (int)dz;
                int sx = x < tx ? 1 : -1, sz = z < tz ? 1 : -1;
                int error = ax - az;
                while (true) {
                    if (index.discover(player, new ChunkKey(current.dimension(), x, z))) newChunks++;
                    if (x == tx && z == tz) return newChunks;
                    int doubleError = 2 * error;
                    if (doubleError > -az) {error -= az; x += sx;}
                    if (doubleError < ax) {error += ax; z += sz;}
                }
            }
        }
        if (index.discover(player, current)) newChunks++;
        return newChunks;
    }

    public void logout(UUID player) { previous.remove(player); }
    public void reset() { previous.clear(); }
}
