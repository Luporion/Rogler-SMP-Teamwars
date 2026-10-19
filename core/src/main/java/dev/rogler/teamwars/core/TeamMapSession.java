package dev.rogler.teamwars.core;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;

import java.util.HashSet;
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
    private Set<ChunkKey> visible = Set.of();
    private long lastExplorationRevision = Long.MIN_VALUE;
    private long generation;
    private Change lastChange = new Change(0, Set.of(), Set.of(), false);

    public TeamMapSession(UUID viewer) { this.viewer = Objects.requireNonNull(viewer); }

    /** A null, missing or inconsistent FTB membership falls back to the viewer's own map. */
    public Change reconcile(TeamAssignmentPolicy.Party ftb, ExplorationIndex index) {
        Objects.requireNonNull(index);
        UUID nextParty = ftb != null && ftb.members().contains(viewer) ? ftb.id() : null;
        Set<UUID> nextMembers = nextParty == null ? Set.of(viewer) : ftb.members();
        boolean newMembership = !Objects.equals(partyId, nextParty) || !nextMembers.equals(authorizedMembers);
        if (!newMembership && lastExplorationRevision == index.revision()) return lastChange;

        Set<ChunkKey> nextVisible = index.visibleTo(nextMembers);
        Set<ChunkKey> revealed = new HashSet<>(nextVisible);
        revealed.removeAll(visible);
        Set<ChunkKey> revoked = new HashSet<>(visible);
        revoked.removeAll(nextVisible);
        // Even if the union is identical, a roster change invalidates any cached team
        // tile provenance and must trigger a generation change at the render layer.
        if (newMembership) generation++;
        partyId = nextParty;
        authorizedMembers = Set.copyOf(nextMembers);
        visible = Set.copyOf(nextVisible);
        lastExplorationRevision = index.revision();
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
