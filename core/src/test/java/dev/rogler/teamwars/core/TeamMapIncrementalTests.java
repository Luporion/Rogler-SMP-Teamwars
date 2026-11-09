package dev.rogler.teamwars.core;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import java.util.Set;
import java.util.UUID;

/** Pure Java regression tests: costs scale with changed personal chunks, not world size. */
public final class TeamMapIncrementalTests {
    private static final UUID A = new UUID(0, 11);
    private static final UUID B = new UUID(0, 12);
    private static final UUID C = new UUID(0, 13);
    private static final UUID TEAM = new UUID(0, 99);
    private static ChunkKey at(int x) {return new ChunkKey("minecraft:overworld", x, 0);}
    private static TeamAssignmentPolicy.Party party(Set<UUID> members) {
        return new TeamAssignmentPolicy.Party(TEAM, "redacted", members);
    }
    public static void main(String[] args) {
        ExplorationIndex idx = new ExplorationIndex();
        for (int x=0; x<30000; x++) idx.discover(A, at(x));
        idx.discover(B, at(200)); // already explored by A: overlapping provenance
        idx.discover(B, at(40000));
        idx.discover(C, at(70000));
        TeamMapSession a = new TeamMapSession(A);
        var joined = a.reconcile(party(Set.of(A,B)),idx);
        assert joined.membershipChanged() && joined.revealed().size()==30001;
        assert a.visibleCount()==30001 && a.members()==2;
        long generation = a.generation();
        // Unrelated team discoveries must not update or clone this session's union.
        var prior = a.lastChange();
        idx.discover(C, at(70001));
        var unrelatedDelta = a.reconcile(party(Set.of(A,B)),idx);
        assert unrelatedDelta.revealed().isEmpty() && unrelatedDelta.revoked().isEmpty();
        assert !unrelatedDelta.membershipChanged() && a.visibleCount()==30001;
        assert a.reconcile(party(Set.of(A,B)),idx)==unrelatedDelta;
        // Only a new personal chunk should be added (including dimension/overlap rules).
        idx.discover(B, at(40001));
        var delta = a.reconcile(party(Set.of(A,B)),idx);
        assert !delta.membershipChanged() && delta.revealed().equals(Set.of(at(40001)));
        assert delta.revoked().isEmpty() && a.visibleCount()==30002 && a.generation()==generation;
        idx.discover(B, at(200)); // no change, duplicate seen by B
        assert a.reconcile(party(Set.of(A,B)),idx)==delta;
        // A saw B's unique terrain later herself: it stays after B leaves.
        idx.discover(A, at(40000));
        delta = a.reconcile(party(Set.of(A,B)),idx);
        assert delta.revealed().isEmpty(); // already visible from B
        var left = a.reconcile(null,idx);
        assert left.membershipChanged() && left.revoked().equals(Set.of(at(40001)));
        assert a.visibleCount()==30001 && idx.personal(A).contains(at(40000));
        assert a.reconcile(null,idx)==left;
        // Journal window overrun must fall back safely to a full authoritative map.
        var renewed = a.reconcile(party(Set.of(A,B)),idx);
        assert renewed.revealed().equals(Set.of(at(40001)));
        for (int x=50000; x<52500; x++) idx.discover(B, at(x));
        assert idx.discoveredSince(B, 2) == null : "Old per-player revision must expire";
        var catchup = a.reconcile(party(Set.of(A,B)),idx);
        assert !catchup.membershipChanged() && catchup.revealed().size()==2500;
        assert a.visibleCount()==32502;
        // FTB party not containing viewer cannot leak any other player's data.
        var hostile = a.reconcile(party(Set.of(B,C)),idx);
        assert hostile.membershipChanged() && a.partyId()==null;
        assert !idx.personal(A).contains(at(70000));
        assert !a.belongsTo(B);
        System.out.println("TeamMapIncrementalTests: 30k initial, irrelevant events, overlap, revocation, 2.5k overflow, privacy OK");
    }
}
