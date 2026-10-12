package dev.rogler.teamwars.core.base;

import dev.rogler.teamwars.core.BaseZone;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Authoritative logical core ledger; no Minecraft imports. All mutations are server-thread only. */
public final class BaseCoreLedger {
    public enum Mode { ISSUED, PLACED, ESCROW }
    public enum PlacementFailure { NONE, NOT_ISSUED, TOKEN_MISMATCH, TIME_EXPIRED, WRONG_DIMENSION, TOO_CLOSE_TO_SPAWN, TOO_FAR_FROM_SPAWN }
    public record Location(String dimension, int x, int y, int z) {
        public Location { Objects.requireNonNull(dimension); }
    }
    public record Entry(UUID teamId, UUID token, Mode mode, long carriedSeconds,
                        Location location, Location lastSafe) {
        public Entry {
            Objects.requireNonNull(teamId);
            Objects.requireNonNull(token);
            Objects.requireNonNull(mode);
            if (carriedSeconds < 0) throw new IllegalArgumentException("Negative time");
            if (mode == Mode.PLACED && location == null) throw new IllegalArgumentException("Placed entry must have location");
            if (mode != Mode.PLACED && location != null) throw new IllegalArgumentException("Unplaced entry cannot have location");
        }
    }
    private final Map<UUID, Entry> entries = new HashMap<>();
    private final BaseZone zone;
    private final int carryLimit;

    public BaseCoreLedger(BaseZone zone, int carryLimit) {
        this.zone = Objects.requireNonNull(zone);
        if (carryLimit < 1) throw new IllegalArgumentException("carryLimit");
        this.carryLimit = carryLimit;
    }
    public Collection<Entry> all() { return Collections.unmodifiableCollection(entries.values()); }
    public Entry get(UUID teamId) { return entries.get(teamId); }
    public int size() { return entries.size(); }
    public boolean legal(Location location) {
        return location != null && "minecraft:overworld".equals(location.dimension())
            && zone.placementAllowed(location.x(), location.z());
    }
    public Entry issue(UUID teamId, UUID token) {
        if (entries.containsKey(teamId)) throw new IllegalStateException("A core already exists for this team");
        if (entries.values().stream().anyMatch(e -> e.token().equals(token)))
            throw new IllegalStateException("Duplicate core token");
        Entry created = new Entry(teamId, token, Mode.ISSUED, 0, null, null);
        entries.put(teamId, created);
        return created;
    }
    /** For restore only. Refuse malformed or repeated identities instead of silently replacing state. */
    public void restore(Entry entry) {
        if (entries.containsKey(entry.teamId()) || entries.values().stream().anyMatch(e -> e.token().equals(entry.token())))
            throw new IllegalArgumentException("Duplicate persisted identity");
        if ((entry.location() != null && !legal(entry.location())) || (entry.lastSafe() != null && !legal(entry.lastSafe())))
            throw new IllegalArgumentException("Invalid persisted base location");
        entries.put(entry.teamId(), entry);
    }
    public PlacementFailure placementFailure(UUID teamId, UUID token, Location location) {
        Entry current = entries.get(teamId);
        if (current == null || current.mode() != Mode.ISSUED) return PlacementFailure.NOT_ISSUED;
        if (!current.token().equals(token)) return PlacementFailure.TOKEN_MISMATCH;
        if (current.carriedSeconds() >= carryLimit) return PlacementFailure.TIME_EXPIRED;
        if (location == null || !"minecraft:overworld".equals(location.dimension()))
            return PlacementFailure.WRONG_DIMENSION;
        if (zone.distance(location.x(), location.z()) < zone.minRadius())
            return PlacementFailure.TOO_CLOSE_TO_SPAWN;
        if (zone.distance(location.x(), location.z()) > zone.maxRadius())
            return PlacementFailure.TOO_FAR_FROM_SPAWN;
        return PlacementFailure.NONE;
    }
    public boolean canPlace(UUID teamId, UUID token, Location location) {
        return placementFailure(teamId, token, location) == PlacementFailure.NONE;
    }
    /**
     * Authoritative PLACED -> ISSUED move, preserving elapsed transport time.
     * A new token invalidates any creative-mode or copied pre-placement item.
     * The world adapter must verify and remove the physical block first, on the server thread.
     */
    public Entry pack(UUID teamId, UUID placedToken, Location expectedLocation, UUID newToken) {
        Entry current = entries.get(teamId);
        if (current == null || current.mode() != Mode.PLACED)
            throw new IllegalStateException("No placed core for this team");
        if (!current.token().equals(placedToken) || !current.location().equals(expectedLocation))
            throw new IllegalStateException("Stored core identity or location mismatch");
        Objects.requireNonNull(newToken, "newToken");
        if (current.token().equals(newToken) || entries.values().stream().anyMatch(e -> e.token().equals(newToken)))
            throw new IllegalStateException("Replacement token must be fresh");
        Entry moved = new Entry(teamId, newToken, Mode.ISSUED, current.carriedSeconds(),
            null, current.location());
        entries.put(teamId, moved);
        return moved;
    }

    /** Explicit OP recovery for PLACED records only if the physical block is confirmed missing. */
    public Entry rescueMissingPlaced(UUID teamId, UUID placedToken, Location expectedLocation) {
        Entry current = entries.get(teamId);
        if (current == null || current.mode() != Mode.PLACED || !current.token().equals(placedToken)
            || !current.location().equals(expectedLocation))
            throw new IllegalStateException("Not the registered placed core");
        Entry escrow = new Entry(teamId, placedToken, Mode.ESCROW, current.carriedSeconds(),
            null, current.location());
        entries.put(teamId, escrow);
        return escrow;
    }

    /** Item destroyed, dropped in hazard or expired. Invalid/stale tokens must not alter the ledger. */
    public boolean rescueToEscrow(UUID teamId, UUID token, Location candidateLastSafe) {
        Entry current = entries.get(teamId);
        if (current == null || current.mode() != Mode.ISSUED || !current.token().equals(token))
            return false;
        Location safe = legal(candidateLastSafe) ? candidateLastSafe : current.lastSafe();
        entries.put(teamId, new Entry(teamId, token, Mode.ESCROW, current.carriedSeconds(), null, safe));
        return true;
    }
    public void placed(UUID teamId, UUID token, Location location) {
        if (!canPlace(teamId, token, location)) throw new IllegalStateException("Placement rejected");
        Entry current = entries.get(teamId);
        entries.put(teamId, new Entry(teamId, token, Mode.PLACED, current.carriedSeconds(), location, location));
    }
    /** Called at most once per second when the genuine token is carried by an online teammate. */
    public Entry advanceCarrier(UUID teamId, UUID token, int seconds, Location verifiedSafeLocation) {
        Entry current = entries.get(teamId);
        if (current == null || current.mode() != Mode.ISSUED || !current.token().equals(token))
            throw new IllegalStateException("No matching issued core");
        if (seconds < 0 || seconds > 60) throw new IllegalArgumentException("Invalid tick interval");
        Location safe = legal(verifiedSafeLocation) ? verifiedSafeLocation : current.lastSafe();
        long elapsed = Math.min((long) carryLimit, current.carriedSeconds() + seconds);
        Mode nextMode = elapsed >= carryLimit ? Mode.ESCROW : Mode.ISSUED;
        Entry next = new Entry(teamId, token, nextMode, elapsed, null, safe);
        entries.put(teamId, next);
        return next;
    }
    /**
     * OP-authorized ownership transfer after FTB confirms the old party no longer exists.
     * Callers MUST verify FTB ownership, physical core (if placed), and claim bookkeeping.
     * Transfer keeps location, spent transport time and safe position, rotates the bearer
     * token, and quarantines any outstanding old-party item in ESCROW.
     */
    public Entry transferOrphan(UUID oldTeam, UUID newTeam, UUID replacementToken) {
        Objects.requireNonNull(oldTeam); Objects.requireNonNull(newTeam); Objects.requireNonNull(replacementToken);
        if (oldTeam.equals(newTeam)) throw new IllegalStateException("A team cannot adopt itself");
        Entry old = entries.get(oldTeam);
        if (old == null) throw new IllegalStateException("Old team has no core");
        if (entries.containsKey(newTeam)) throw new IllegalStateException("Destination team already has a core");
        if (old.token().equals(replacementToken) || entries.values().stream().anyMatch(e -> e.token().equals(replacementToken)))
            throw new IllegalStateException("Replacement token must be fresh");
        Mode mode = old.mode() == Mode.ISSUED ? Mode.ESCROW : old.mode();
        Entry transferred = new Entry(newTeam, replacementToken, mode, old.carriedSeconds(), old.location(), old.lastSafe());
        entries.remove(oldTeam);
        entries.put(newTeam, transferred);
        return transferred;
    }

    /** Server-only authorized raid victory. Converts the placed core into recoverable
     * escrow exactly once. A successful kill always revokes active placement and
     * grants the defeated team a fresh transport budget AFTER explicit recovery.
     */
    public Entry defeatPlaced(UUID defender, UUID expectedToken, Location expectedLocation) {
        Entry old=entries.get(defender);
        if(old==null || old.mode()!=Mode.PLACED || !old.token().equals(expectedToken)
            || !old.location().equals(expectedLocation))
            throw new IllegalStateException("Core already defeated, missing or replaced");
        Entry next=new Entry(defender,old.token(),Mode.ESCROW,0L,null,old.location());
        entries.put(defender,next);
        return next;
    }

    /** Admin recovery after timeout. Old token is invalidated; no replacement before explicit authorization. */
    public Entry reissueFromEscrow(UUID teamId, UUID replacementToken) {
        Entry current = entries.get(teamId);
        if (current == null || current.mode() != Mode.ESCROW) throw new IllegalStateException("Not in escrow");
        if (entries.values().stream().anyMatch(e -> e.token().equals(replacementToken)))
            throw new IllegalStateException("Duplicate replacement token");
        // Escrow must not refresh the one-hour carry budget. An expired core gets
        // exactly one placement attempt before the timer returns to escrow.
        long restoredElapsed = Math.min((long) carryLimit - 1L, current.carriedSeconds());
        Entry next = new Entry(teamId, replacementToken, Mode.ISSUED, restoredElapsed, null, current.lastSafe());
        entries.put(teamId, next);
        return next;
    }
}
