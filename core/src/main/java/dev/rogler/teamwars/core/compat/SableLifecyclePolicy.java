package dev.rogler.teamwars.core.compat;

/** Pure safety gates for the narrowly scoped Sable 2.0.6 heatmap preflight.
 * No compatibility claim is made for other Sable versions or during another
 * addon's own split pipeline. Splitting remains enabled in Sable's config.
 */
public final class SableLifecyclePolicy {
    public static final String SUPPORTED_SABLE_VERSION = "2.0.6";
    private SableLifecyclePolicy() {}

    public enum Reason { OFF, SABLE_ABSENT, VERSION_NOT_VERIFIED, DESTRUCTIVE_PRESENT, ACTIVE }

    public static Reason eligibility(boolean enabled, String sableVersion, boolean destructivePresent) {
        if (!enabled) return Reason.OFF;
        if (sableVersion == null || sableVersion.isBlank()) return Reason.SABLE_ABSENT;
        // Fail closed on unknown Sable versions (not just matching a major/minor prefix).
        if (!SUPPORTED_SABLE_VERSION.equals(sableVersion)) return Reason.VERSION_NOT_VERIFIED;
        if (destructivePresent) return Reason.DESTRUCTIVE_PRESENT;
        return Reason.ACTIVE;
    }

    public static boolean skipTick(boolean removedOwner) { return removedOwner; }
    public static boolean skipSplit(boolean removedOwner, boolean removedAnchor) {
        return removedOwner || removedAnchor;
    }

    /** Sable's split() intentionally does not assemble a sole group that already
     * contains the whole sublevel. Keep the exact no-op fast path intact. */
    public static boolean assemblesAnyGroup(int groupCount, int totalSplitBlocks, int solidCount) {
        return groupCount > 0 && !(groupCount == 1 && totalSplitBlocks == solidCount);
    }

    /** When all solid blocks are fragmented, Sable retains the largest component
     * and only assembles the others. (Used solely for conservative preflight.) */
    public static boolean retainLargest(int groupCount, int totalSplitBlocks, int solidCount) {
        return groupCount > 1 && totalSplitBlocks > 0 && totalSplitBlocks == solidCount;
    }
}
