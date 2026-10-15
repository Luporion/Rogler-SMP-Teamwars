package dev.rogler.teamwars.core.base;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded, in-memory compensation policy for externally destroyed cores.
 * Never modifies the world; the server adapter checks loaded chunks, placeability,
 * team ownership, FTB claim provenance and data persistence separately.
 * This is not a way to keep repairing through an active nuclear explosion.
 */
public final class CoreRecoveryPolicy {
    public enum Decision { WAIT, RESTORE, QUARANTINE }
    public static final long QUIET_PERIOD_MS = 5_000L;
    public static final long ATTEMPT_WINDOW_MS = 120_000L;
    public static final int MAX_AUTO_RESTORES = 2;

    private static final class Record {
        long missingSince = -1;
        long firstRestore = -1;
        int restores = 0;
    }
    private final Map<UUID, Record> records = new HashMap<>();

    public Decision missingAir(UUID team, long nowMs) {
        Record r = records.computeIfAbsent(team, ignored -> new Record());
        expireWindow(r, nowMs);
        if (r.restores >= MAX_AUTO_RESTORES) return Decision.QUARANTINE;
        if (r.missingSince < 0 || nowMs < r.missingSince) {
            r.missingSince = nowMs;
            return Decision.WAIT;
        }
        if (nowMs - r.missingSince < QUIET_PERIOD_MS) return Decision.WAIT;
        if (r.restores == 0) r.firstRestore = nowMs;
        r.restores++;
        r.missingSince = -1;
        return Decision.RESTORE;
    }

    /** A healthy core preserves recent repair counts to catch repeated nuke aftereffects. */
    public void healthy(UUID team, long nowMs) {
        Record r = records.get(team);
        if (r == null) return;
        r.missingSince = -1;
        expireWindow(r, nowMs);
        if (r.restores == 0) records.remove(team);
    }

    /** Chunk unloaded: do not count unloaded time as stable missing air. */
    public void unloaded(UUID team) {
        Record r = records.get(team);
        if (r != null) r.missingSince = -1;
    }

    /** Core packed, moved or transferred to escrow: discard transient observations. */
    public void resolved(UUID team) { records.remove(team); }

    private static void expireWindow(Record r, long nowMs) {
        if (r.firstRestore >= 0 && (nowMs < r.firstRestore || nowMs - r.firstRestore >= ATTEMPT_WINDOW_MS)) {
            r.firstRestore = -1;
            r.restores = 0;
        }
    }
}
