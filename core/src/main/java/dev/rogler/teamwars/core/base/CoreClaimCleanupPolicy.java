package dev.rogler.teamwars.core.base;

/** Management of a *previously recorded* auto-claim receipt; never unclaim foreign claims. */
public final class CoreClaimCleanupPolicy {
    private CoreClaimCleanupPolicy() {}
    public enum Action { UNCLAIM_OWN, FORGET_OBSOLETE_RECEIPT }
    public static Action decide(boolean claimExists, boolean ownerIsOriginalTeam) {
        return claimExists && ownerIsOriginalTeam ? Action.UNCLAIM_OWN : Action.FORGET_OBSOLETE_RECEIPT;
    }
}
