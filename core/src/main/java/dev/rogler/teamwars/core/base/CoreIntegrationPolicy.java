package dev.rogler.teamwars.core.base;

/** Conservative contracts for future optional Create and AE2 adapters.
 * A policy is not a working kinetic-grid provider or an AE2 grid node.
 */
public final class CoreIntegrationPolicy {
    private CoreIntegrationPolicy() {}
    public static final int CREATE_RPM = 16;
    public static final int CREATE_BASIC_SU = 256;
    public static final int CREATE_UPGRADED_SU = 512;
    public static int proposedStressCapacity(int level) {
        return switch (level) {
            case 0 -> 0;
            case 1 -> CREATE_BASIC_SU;
            case 2 -> CREATE_UPGRADED_SU;
            default -> throw new IllegalArgumentException("invalid kinetic upgrade level");
        };
    }
    /** Capability providers cannot identify a requesting team: never expose private
     * 27-slot storage to a global item capability until caller security is solved. */
    public static boolean itemAutomationAllowed(boolean callerIdentityVerified) {
        return callerIdentityVerified;
    }
}
