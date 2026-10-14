package dev.rogler.teamwars.core.base;

/** Conservative core/claim reconciliation rules. Never load a chunk or overwrite
 * a different block to "repair" a missing Core. The world adapter owns effects. */
public final class CoreIntegrityPolicy {
    private CoreIntegrityPolicy() {}
    public enum Action { HEALTHY, DEFER_UNLOADED, RESTORE_AIR, ESCROW_RELEASE, RELEASE_STALE_CLAIMS }
    public static Action decide(boolean placed, boolean chunkLoaded, boolean registeredBlock,
                                boolean air, boolean hasAutoClaims) {
        if (!placed) return hasAutoClaims ? Action.RELEASE_STALE_CLAIMS : Action.HEALTHY;
        if (!chunkLoaded) return Action.DEFER_UNLOADED;
        if (registeredBlock) return Action.HEALTHY;
        return air ? Action.RESTORE_AIR : Action.ESCROW_RELEASE;
    }
}
