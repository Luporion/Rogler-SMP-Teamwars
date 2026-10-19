package dev.rogler.teamwars.core;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import java.util.Set;
import java.util.UUID;

public final class TeamMapSessionTests {
    private static final UUID A=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B=UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID C=UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID T=UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final ChunkKey ONE=new ChunkKey("minecraft:overworld",0,0);
    private static final ChunkKey TWO=new ChunkKey("minecraft:overworld",2,-3);
    private static final ChunkKey BOTH=new ChunkKey("minecraft:overworld",-9,1);
    private static final ChunkKey NETHER=new ChunkKey("minecraft:the_nether",2,-3);

    public static void main(String[] args) {
        ExplorationIndex data = new ExplorationIndex();
        assert data.discover(A,ONE);assert !data.discover(A,ONE);
        assert data.revision()==1;
        data.discover(A,BOTH);data.discover(B,BOTH);data.discover(B,TWO);data.discover(C,NETHER);
        TeamMapSession a=new TeamMapSession(A);
        TeamAssignmentPolicy.Party party=new TeamAssignmentPolicy.Party(T,"Team", Set.of(A,B));
        var joined=a.reconcile(party,data);
        assert joined.membershipChanged();
        assert joined.revealed().equals(Set.of(ONE,BOTH,TWO));
        assert joined.revoked().isEmpty();
        assert a.members()==2 && a.visibleCount()==3 && T.equals(a.partyId());
        assert a.reconcile(party,data)==joined : "unchanged revision/roster should not recompute";
        long generation=a.generation();
        var left=a.reconcile(null,data);
        assert left.membershipChanged() && left.revoked().equals(Set.of(TWO));
        assert left.revealed().isEmpty();
        assert a.generation()>generation && a.visibleCount()==2;
        assert data.personal(B).contains(TWO) : "revocation must not destroy original provenance";
        assert !data.personal(A).contains(TWO);
        assert a.reconcile(new TeamAssignmentPolicy.Party(T,"Fake",Set.of(B,C)),data)==left;
        assert a.partyId()==null && a.members()==1 : "an unrelated FTB party must fail closed";
        data.discover(B,NETHER);
        assert a.reconcile(null,data).revoked().isEmpty() : "another player's exploration is invisible to solo";
        assert !a.belongsTo(B) && a.belongsTo(A);
        var rejoin=a.reconcile(party,data);
        assert rejoin.revealed().equals(Set.of(TWO,NETHER));
        assert a.reconcile(party,data)==rejoin : "a stable roster must not create another delta";
        System.out.println("TeamMapSessionTests passed (team additions, overlap, revocation, rejoin, dimensions, stale members)");
    }
}
