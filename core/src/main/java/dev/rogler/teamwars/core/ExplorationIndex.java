package dev.rogler.teamwars.core;

import java.util.*;

/** Server-authoritative per-player exploration provenance. Never mark chunk explored from Chunky/pregen. */
public final class ExplorationIndex {
    private final Map<UUID, Set<ChunkKey>> personal = new HashMap<>();
    private long revision;
    /** Only retain a bounded, per-player change history. A session that falls behind
     * the window safely falls back to a full authoritative rebuild. */
    private static final int MAX_RECENT_CHANGES = 2048;
    private final Map<UUID, Long> playerRevisions = new HashMap<>();
    private final Map<UUID, ArrayDeque<Discovery>> changes = new HashMap<>();
    private record Discovery(long revision, ChunkKey chunk) {}

    public long personalRevision(UUID player) {
        return playerRevisions.getOrDefault(player, 0L);
    }

    /** Null means the bounded journal cannot answer; caller MUST rebuild from full data.
     * The journal is deliberately not persisted: loaded sessions build a fresh baseline. */
    public Set<ChunkKey> discoveredSince(UUID player, long afterRevision) {
        long latest = personalRevision(player);
        if (afterRevision < 0 || afterRevision > latest) return null;
        if (afterRevision == latest) return Set.of();
        ArrayDeque<Discovery> journal = changes.get(player);
        if (journal == null || journal.isEmpty() || afterRevision < journal.peekFirst().revision() - 1)
            return null;
        Set<ChunkKey> delta = new HashSet<>();
        for (Discovery change : journal)
            if (change.revision() > afterRevision) delta.add(change.chunk());
        return delta;
    }

    /** For lightweight status reporting; does not clone tens of thousands of chunks. */
    public long totalPersonalRecords() {
        long total = 0;
        for (Set<ChunkKey> entries : personal.values()) total += entries.size();
        return total;
    }


    /** Monotonic in this session: advances only for newly *personally visited* chunks. */
    public long revision() { return revision; }

    /** True only if this player did not previously own this exploration sample. */
    public boolean discover(UUID player, ChunkKey chunk) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(chunk);
        boolean discovered = personal.computeIfAbsent(player, ignored -> new HashSet<>()).add(chunk);
        if (discovered) {
            revision++;
            long next = playerRevisions.merge(player, 1L, Long::sum);
            ArrayDeque<Discovery> journal = changes.computeIfAbsent(player, ignored -> new ArrayDeque<>());
            journal.addLast(new Discovery(next, chunk));
            if (journal.size() > MAX_RECENT_CHANGES) journal.removeFirst();
        }
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
