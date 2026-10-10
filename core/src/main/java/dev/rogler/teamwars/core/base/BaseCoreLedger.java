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
    public boolean canPlace(UUID teamId, UUID token, Location location) {
        Entry current = entries.get(teamId);
        return current != null && current.mode() == Mode.ISSUED && current.token().equals(token)
            && current.carriedSeconds() < carryLimit && legal(location);
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
    /** Admin recovery after timeout. Old token is invalidated; no replacement before explicit authorization. */
    public Entry reissueFromEscrow(UUID teamId, UUID replacementToken) {
        Entry current = entries.get(teamId);
        if (current == null || current.mode() != Mode.ESCROW) throw new IllegalStateException("Not in escrow");
        if (entries.values().stream().anyMatch(e -> e.token().equals(replacementToken)))
            throw new IllegalStateException("Duplicate replacement token");
        Entry next = new Entry(teamId, replacementToken, Mode.ISSUED, 0, null, current.lastSafe());
        entries.put(teamId, next);
        return next;
    }
}
