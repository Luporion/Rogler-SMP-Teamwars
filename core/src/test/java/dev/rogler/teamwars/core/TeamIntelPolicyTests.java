package dev.rogler.teamwars.core;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TeamIntelPolicyTests {
    static int checks;
    static void ok(boolean yes, String reason) { if (!yes) throw new AssertionError(reason); checks++; }

    public static void main(String[] args) {
        UUID a=UUID.randomUUID(), b=UUID.randomUUID(), c=UUID.randomUUID(), d=UUID.randomUUID();
        UUID red=UUID.randomUUID(), blue=UUID.randomUUID();
        var redParty = new TeamAssignmentPolicy.Party(red,"Red",Set.of(a,b,c));
        var blueParty = new TeamAssignmentPolicy.Party(blue,"Blue",Set.of(d));
        var online = List.of(
            new TeamIntelPolicy.Presence(a,"A","minecraft:overworld",10,20),
            new TeamIntelPolicy.Presence(b,"B","minecraft:overworld",-5,-20),
            new TeamIntelPolicy.Presence(c,"C","minecraft:the_nether",1,2),
            new TeamIntelPolicy.Presence(d,"ENEMY","minecraft:overworld",9999,9999));
        var roster = Map.of(a,redParty,b,redParty,c,redParty,d,blueParty);
        var visible = TeamIntelPolicy.authorized(a,redParty,roster,online);
        ok(visible.size()==2,"both teammates present");
        ok(visible.stream().noneMatch(p->p.playerId().equals(d)),"no enemy position");
        ok(visible.stream().noneMatch(p->p.playerId().equals(a)),"no self marker");
        ok(visible.stream().anyMatch(p->p.x()==-5),"negative coordinates stay valid");
        ok(visible.stream().anyMatch(p->p.dimension().equals("minecraft:the_nether")),"dimension preserved");
        ok(TeamIntelPolicy.authorized(a,null,roster,online).isEmpty(),"fail closed without party");
        ok(TeamIntelPolicy.authorized(a,redParty,Map.of(),online).isEmpty(),"fail closed without viewer identity");
        ok(TeamIntelPolicy.authorized(a,redParty,Map.of(a,blueParty),online).isEmpty(),"fail closed mismatch");
        var stale = TeamIntelPolicy.authorized(a,redParty,Map.of(a,redParty,b,blueParty,c,redParty,d,blueParty),online);
        ok(stale.size()==1 && stale.getFirst().playerId().equals(c),"stale member refused");
        ok(TeamIntelPolicy.authorized(d,blueParty,roster,online).isEmpty(),"opposing solo party has no red coordinates");
        ok(TeamIntelPolicy.safeName("hi\u00a7x\nthere").equals("hixthere"),"format codes filtered");
        ok(TeamIntelPolicy.safeDimension("a".repeat(140)).equals("unknown"),"dimension bounded");

        var client = new TeamIntelClientState();
        var s1 = new TeamIntelClientState.Snapshot(1,"minecraft:overworld",0,0,0,5000,visible);
        var s2 = new TeamIntelClientState.Snapshot(2,"minecraft:overworld",0,0,0,5000,List.of());
        ok(client.accept(s1,10),"first accepted");
        ok(client.active(11).teammates().size()==2,"stored ephemeral client snapshot");
        ok(client.accept(s2,12),"revoke accepted");
        ok(client.active(12).teammates().isEmpty(),"revoke erases all coordinates");
        ok(!client.accept(s1,13),"old snapshot cannot resurrect team data");
        ok(client.active(215)==null,"stale snapshots expire");
        client.reset();
        ok(client.active(10)==null,"logout clears data");
        ok(client.accept(s1,1),"new login sequence restarts");
        System.out.println("TeamIntelPolicyTests: " + checks + " checks OK");
    }
}
