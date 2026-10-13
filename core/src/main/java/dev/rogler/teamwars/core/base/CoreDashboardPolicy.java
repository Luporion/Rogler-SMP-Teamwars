package dev.rogler.teamwars.core.base;

/** Transport-agnostic contract for server-authoritative GUI actions.
 * No client-supplied price, level, team, inventory or coordinates are trusted.
 */
public final class CoreDashboardPolicy {
    private CoreDashboardPolicy() {}
    public static final int ARMOR=1, ENERGY=2, CREATE=3, MANUAL_BASE=100;
    public static boolean allowed(int action,int manualCount,boolean ownPlacedCore,
                                  boolean survival,boolean raidActive) {
        if(!ownPlacedCore) return false;
        if(action>=MANUAL_BASE) return action-MANUAL_BASE < manualCount;
        if(action==ARMOR || action==ENERGY || action==CREATE)
            return survival && !raidActive;
        return false;
    }
    public static String section(int index) {
        return switch(index) {
            case 0 -> "STATUS";
            case 1 -> "UPGRADES";
            case 2 -> "ENERGY";
            case 3 -> "GUIDES";
            case 4 -> "FUTURE";
            default -> "INVALID";
        };
    }
}
