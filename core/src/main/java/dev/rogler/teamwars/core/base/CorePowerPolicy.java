package dev.rogler.teamwars.core.base;

/** Deterministic energy limits; no Minecraft or foreign mod APIs. */
public final class CorePowerPolicy {
    private CorePowerPolicy() {}
    public static final int MAX_LEVEL = 2;
    public static final int SLOTS = 27;
    public static final int MAX_FE = 20_000;
    public static final int MAX_EXTRACT_PER_CALL = 160;

    public static int generationPerTick(int level) {
        return switch (level) {
            case 0 -> 20;
            case 1 -> 40;
            case 2 -> 80;
            default -> throw new IllegalArgumentException("energy level must be 0..2");
        };
    }
    public static int redstoneForNext(int level) {
        return switch (level) {
            case 0 -> 16;
            case 1 -> 32;
            default -> throw new IllegalArgumentException("no further power upgrade");
        };
    }
    public static int ironForNext(int level) {
        return switch (level) {
            case 0 -> 8;
            case 1 -> 0;
            default -> throw new IllegalArgumentException("no further power upgrade");
        };
    }
    public static int goldForNext(int level) {
        return switch (level) {
            case 0 -> 0;
            case 1 -> 8;
            default -> throw new IllegalArgumentException("no further power upgrade");
        };
    }
    public static int clampEnergy(long value) { return (int) Math.max(0L, Math.min(MAX_FE, value)); }
    public static int generate(int energy, int level, boolean placed) {
        return clampEnergy((long)energy + (placed ? generationPerTick(level) : 0));
    }
    public static int extractable(int energy, int requested) {
        return Math.min(Math.max(0, energy), Math.min(Math.max(0, requested), MAX_EXTRACT_PER_CALL));
    }
    public static boolean canUpgrade(int level, int redstone, int iron, int gold,
                                     boolean placed, boolean raidActive) {
        return placed && !raidActive && level >= 0 && level < MAX_LEVEL
            && redstone >= redstoneForNext(level)
            && iron >= ironForNext(level) && gold >= goldForNext(level);
    }
}
