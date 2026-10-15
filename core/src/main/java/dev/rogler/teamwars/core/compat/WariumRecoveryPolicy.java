package dev.rogler.teamwars.core.compat;

import java.util.Locale;

/** Pure validation and decision layer. No Minecraft classes or world accesses. */
public final class WariumRecoveryPolicy {
    private WariumRecoveryPolicy() {}

    public enum Mode { OFF, LOG_ONLY, BLOCK_DISK_LOADED, BLOCK_ALL_NEW }
    public enum Action { IGNORE, OBSERVE, DENY_JOIN }

    /** Exact observed Warium class, not a broad 'nuke' substring match. Registry namespace must also match. */
    public static boolean isFusionBlast(String registryNamespace, String simpleClassName,
                                        String registryId, String expectedId) {
        if (!"wariumce".equals(registryNamespace) || !"FusionBlastEntityEntity".equals(simpleClassName))
            return false;
        if (registryId == null || !registryId.startsWith("wariumce:")) return false;
        return expectedId == null || expectedId.isBlank() || expectedId.equals(registryId);
    }

    public static Mode parseMode(String text) {
        if (text == null) throw new IllegalArgumentException("mode missing");
        try { return Mode.valueOf(text.strip().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Unknown Warium recovery mode: " + text); }
    }

    public static Action decide(Mode mode, boolean target, boolean loadedFromDisk) {
        if (!target || mode == Mode.OFF) return Action.IGNORE;
        if (mode == Mode.LOG_ONLY) return Action.OBSERVE;
        if (mode == Mode.BLOCK_DISK_LOADED) return loadedFromDisk ? Action.DENY_JOIN : Action.OBSERVE;
        return loadedFromDisk ? Action.OBSERVE : Action.DENY_JOIN; // BLOCK_ALL_NEW
    }
}
