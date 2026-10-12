package dev.rogler.teamwars.core.base;

/** Pure server-side core upgrade rules. Never accept a client-declared level or cost. */
public final class CoreUpgradePolicy {
    private CoreUpgradePolicy() {}
    public static final int MAX_ARMOR_LEVEL = 2;
    public static final int BASE_HP = 10;
    public static int maxHealth(int armorLevel) {
        if (armorLevel < 0 || armorLevel > MAX_ARMOR_LEVEL)
            throw new IllegalArgumentException("armor level must be 0..2");
        return BASE_HP + 5 * armorLevel;
    }
    /** Cost to BUY the next level; current level 2 cannot upgrade. */
    public static int diamondsForNext(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= MAX_ARMOR_LEVEL) throw new IllegalArgumentException("no next tier");
        return currentLevel == 0 ? 8 : 16;
    }
    public static int netheriteForNext(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= MAX_ARMOR_LEVEL) throw new IllegalArgumentException("no next tier");
        return currentLevel == 0 ? 0 : 1;
    }
    public static boolean canBuy(int level, int diamonds, int netherite, boolean placed, boolean raidOpen) {
        return placed && !raidOpen && level >= 0 && level < MAX_ARMOR_LEVEL
            && diamonds >= diamondsForNext(level) && netherite >= netheriteForNext(level);
    }
}
