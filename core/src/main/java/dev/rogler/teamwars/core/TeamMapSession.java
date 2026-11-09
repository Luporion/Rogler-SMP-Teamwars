package dev.rogler.teamwars.core;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Per-viewer *authorization snapshot*, not a persistent copy of a team's map.
 * A viewer never inherits another player's personal discovery record.
 * The renderer may only consume revealed/revoked from this particular viewer's session.
 * A renderer must purge cached team tiles on session reset/change; this class does not
 * claim that a client-renderer exists.
 */
public final class TeamMapSession {
    private final UUID viewer;
    private UUID partyId;
    private Set<UUID> authorizedMembers = Set.of();
    private Set<ChunkKey> visible = new HashSet<>();
    private Map<UUID, Long> memberRevisions = new HashMap<>();
    private ExplorationIndex dataSource;
    private long lastSeenGlobalRevision = Long.MIN_VALUE;
    private long generation;
    private Change lastChange = new Change(0, Set.of(), Set.of(), false);

    public TeamMapSession(UUID viewer) { this.viewer = Objects.requireNonNull(viewer); }

    /** A null, missing or inconsistent FTB membership falls back to the viewer's own map.
     * Stable rosters process only new personal discoveries (no complete union per tick).
     * Membership changes deliberately rebuild from canonical records to revoke access. */
    public Change reconcile(TeamAssignmentPolicy.Party ftb, ExplorationIndex index) {
        Objects.requireNonNull(index);
        UUID nextParty = ftb != null && ftb.members().contains(viewer) ? ftb.id() : null;
        Set<UUID> nextMembers = nextParty == null ? Set.of(viewer) : ftb.members();
        boolean newMembership = dataSource != index || !Objects.equals(partyId, nextParty)
            || !nextMembers.equals(authorizedMembers);

        if (!newMembership) {
            boolean changed = false;
            for (UUID member : authorizedMembers) {
                if (memberRevisions.getOrDefault(member, -1L) != index.personalRevision(member)) {
                    changed = true;
                    break;
                }
            }
            // Discoveries by other teams do NOT trigger an O(world-size) rebuild.
            if (!changed) {
                // Preserve the existing delta API: an unrelated new discovery
                // produces an empty delta, but never scans the visible union.
                if (lastSeenGlobalRevision != index.revision()) {
                    lastSeenGlobalRevision = index.revision();
                    lastChange = new Change(generation, Set.of(), Set.of(), false);
                }
                return lastChange;
            }

            Set<ChunkKey> revealed = new HashSet<>();
            boolean needsRebuild = false;
            for (UUID member : authorizedMembers) {
                long oldRevision = memberRevisions.getOrDefault(member, -1L);
                long newRevision = index.personalRevision(member);
                if (oldRevision == newRevision) continue;
                Set<ChunkKey> additions = index.discoveredSince(member, oldRevision);
                if (additions == null) {
                    needsRebuild = true; // journal overflow, never silently lose a discovery
                    break;
                }
                for (ChunkKey chunk : additions)
                    if (visible.add(chunk)) revealed.add(chunk);
                memberRevisions.put(member, newRevision);
            }
            if (!needsRebuild) {
                lastSeenGlobalRevision = index.revision();
                lastChange = new Change(generation, Set.copyOf(revealed), Set.of(), false);
                return lastChange;
            }
        }

        // Authorization/roster changes or an expired history require a full rebuild.
        // Every newly authorized member is re-evaluated from server-owned provenance.
        Set<ChunkKey> nextVisible = index.visibleTo(nextMembers);
        Set<ChunkKey> revealed = new HashSet<>(nextVisible);
        revealed.removeAll(visible);
        Set<ChunkKey> revoked = new HashSet<>(visible);
        revoked.removeAll(nextVisible);
        if (newMembership) generation++;
        partyId = nextParty;
        authorizedMembers = Set.copyOf(nextMembers);
        visible = new HashSet<>(nextVisible);
        dataSource = index;
        lastSeenGlobalRevision = index.revision();
        memberRevisions = new HashMap<>();
        for (UUID member : nextMembers)
            memberRevisions.put(member, index.personalRevision(member));
        lastChange = new Change(generation, Set.copyOf(revealed), Set.copyOf(revoked), newMembership);
        return lastChange;
    }

    public UUID viewer() {return viewer;}
    public UUID partyId() {return partyId;}
    public int members() {return authorizedMembers.size();}
    public int visibleCount() {return visible.size();}
    public long generation() {return generation;}
    public Change lastChange() {return lastChange;}
    /** Never reuse a visibility snapshot for a different player. */
    public boolean belongsTo(UUID player) {return viewer.equals(player);}
    public record Change(long generation, Set<ChunkKey> revealed, Set<ChunkKey> revoked, boolean membershipChanged) {}
}
