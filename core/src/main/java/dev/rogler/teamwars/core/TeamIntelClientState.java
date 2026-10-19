package dev.rogler.teamwars.core;

import java.util.List;
import java.util.Objects;

/**
 * Volatile client session. NO disk persistence and NO retaining teammate coordinates
 * after logout, reconnect, token change, or stale-snapshot timeout.
 */
public final class TeamIntelClientState {
    public record Snapshot(long sequence, String dimension, int spawnX, int spawnZ,
                           int minRadius, int maxRadius, List<TeamIntelPolicy.Presence> teammates) {
        public Snapshot {
            Objects.requireNonNull(dimension);
            teammates = List.copyOf(teammates);
            if (minRadius < 0 || maxRadius < minRadius || maxRadius > 30000000)
                throw new IllegalArgumentException("invalid base radius");
            if (teammates.size() > TeamIntelPolicy.MAX_MARKERS)
                throw new IllegalArgumentException("too many team markers");
        }
    }

    private Snapshot current;
    private long lastSequence = -1;
    private long receivedTick = -1;

    /** Strictly monotonic within this login; old packets must never bring removed teammates back. */
    public boolean accept(Snapshot snapshot, long tick) {
        if (snapshot.sequence() <= lastSequence) return false;
        current = snapshot;
        lastSequence = snapshot.sequence();
        receivedTick = tick;
        return true;
    }

    public Snapshot active(long tick) {
        // The server sends every 2 seconds. No team coordinates survive a 10-second outage.
        return current != null && tick >= receivedTick && tick - receivedTick <= 200 ? current : null;
    }

    public void reset() {
        current = null;
        lastSequence = -1;
        receivedTick = -1;
    }
}
