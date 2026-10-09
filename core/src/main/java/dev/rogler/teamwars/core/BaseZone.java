package dev.rogler.teamwars.core;

/** All distances use X/Z and long arithmetic to avoid integer overflow near world border. */
public record BaseZone(int spawnX, int spawnZ, int minRadius, int maxRadius) {
    public BaseZone {
        if (minRadius < 0 || maxRadius <= minRadius) throw new IllegalArgumentException("Invalid radii");
    }
    public boolean placementAllowed(int x, int z) {
        long dx = (long)x - spawnX, dz = (long)z - spawnZ;
        long sq = dx * dx + dz * dz;
        return sq >= (long)minRadius * minRadius && sq <= (long)maxRadius * maxRadius;
    }
    public double distance(int x, int z) {
        return Math.hypot((long)x - spawnX, (long)z - spawnZ);
    }
    public double remaining(int x, int z) { return maxRadius - distance(x, z); }
}
