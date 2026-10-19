package dev.rogler.teamwars.core;

import java.util.*;

/** Server-authoritative per-player exploration provenance. Never mark chunk explored from Chunky/pregen. */
public final class ExplorationIndex {
    private final Map<UUID, Set<ChunkKey>> personal = new HashMap<>();
    private long revision;

    /** Monotonic in this session: advances only for newly *personally visited* chunks. */
    public long revision() { return revision; }

    /** True only if this player did not previously own this exploration sample. */
    public boolean discover(UUID player, ChunkKey chunk) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(chunk);
        boolean discovered = personal.computeIfAbsent(player, ignored -> new HashSet<>()).add(chunk);
        if (discovered) revision++;
        return discovered;
    }
    public Set<ChunkKey> personal(UUID player) {
        return Set.copyOf(personal.getOrDefault(player, Set.of()));
    }
    /** Aggregates only *current* members; does not transfer ownership to other members. */
    public Set<ChunkKey> visibleTo(Set<UUID> currentMembers) {
        Set<ChunkKey> result = new HashSet<>();
        for (UUID id : currentMembers) result.addAll(personal.getOrDefault(id, Set.of()));
        return Set.copyOf(result);
    }
    /** Delta to send after membership changes; a renderer must also remove stale cached tiles. */
    public VisibilityDelta delta(Set<UUID> before, Set<UUID> after) {
        Set<ChunkKey> old = new HashSet<>(visibleTo(before));
        Set<ChunkKey> next = new HashSet<>(visibleTo(after));
        Set<ChunkKey> added = new HashSet<>(next);
        added.removeAll(old);
        old.removeAll(next);
        return new VisibilityDelta(Set.copyOf(added), Set.copyOf(old));
    }
    public record VisibilityDelta(Set<ChunkKey> revealed, Set<ChunkKey> revoked) {}
    public Map<UUID, Set<ChunkKey>> exportSnapshot() {
        Map<UUID, Set<ChunkKey>> copy = new HashMap<>();
        personal.forEach((id, chunks) -> copy.put(id, Set.copyOf(chunks)));
        return Map.copyOf(copy);
    }
    public static ExplorationIndex fromSnapshot(Map<UUID, Set<ChunkKey>> snapshot) {
        ExplorationIndex index = new ExplorationIndex();
        snapshot.forEach((id, chunks) -> index.personal.put(id, new HashSet<>(chunks)));
        return index;
    }
}
