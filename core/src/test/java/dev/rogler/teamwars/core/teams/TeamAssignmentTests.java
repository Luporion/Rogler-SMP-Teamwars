package dev.rogler.teamwars.core.teams;

import java.util.Set;
import java.util.UUID;

public final class TeamAssignmentTests {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        UUID alice=UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bob=UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID team=UUID.fromString("00000000-0000-0000-0000-000000000003");
        TeamAssignmentPolicy.Party party = new TeamAssignmentPolicy.Party(team, "Alpha",Set.of(alice,bob));
        var a=TeamAssignmentPolicy.assign(alice,party,4);
        var b=TeamAssignmentPolicy.assign(bob,party,4);
        check(a.validParty(),"valid party");
        check(a.scoreboardKey().equals(b.scoreboardKey()),"party friends same scoreboard team");
        check(a.partyId().equals(team),"identity is canonical FTB UUID");
        check(a.scoreboardKey().length()==16,"scoreboard length");
        check(a.displayName().equals("Alpha"),"visible name");
        check(!TeamAssignmentPolicy.assign(alice,null,4).validParty(),"no FTB party solo");
        check(!TeamAssignmentPolicy.assign(UUID.randomUUID(),party,4).validParty(),"member mismatch fail closed");
        check(!TeamAssignmentPolicy.assign(alice, new TeamAssignmentPolicy.Party(team,"Huge",Set.of(alice,bob,UUID.randomUUID())),2).validParty(),"oversize fail closed");
        check(TeamAssignmentPolicy.assign(alice,party,4).scoreboardKey().equals(a.scoreboardKey()),"stable across restart");
        check(!TeamAssignmentPolicy.solo(alice,"none").scoreboardKey().equals(TeamAssignmentPolicy.solo(bob,"none").scoreboardKey()),"solo players isolated");
        check(!TeamAssignmentPolicy.solo(alice,"none").scoreboardKey().equals(a.scoreboardKey()),"solo separate namespace");
        check(TeamAssignmentPolicy.assign(alice,new TeamAssignmentPolicy.Party(team,"New Name",Set.of(alice,bob)),4).scoreboardKey().equals(a.scoreboardKey()),"rename preserves identity");
        check(!TeamAssignmentPolicy.assign(bob,new TeamAssignmentPolicy.Party(team,"Solo",Set.of(alice)),4).validParty(),"member who left gets solo");
        check(!TeamAssignmentPolicy.assign(alice,new TeamAssignmentPolicy.Party(team,"",Set.of(alice)),4).displayName().isBlank(),"blank name fallback");
        boolean invalid = false;
        try { TeamAssignmentPolicy.assign(alice,party,0); } catch(IllegalArgumentException good) { invalid=true; }
        check(invalid,"reject invalid max limit");
        System.out.println("v0.1.0 TeamAssignmentTests: " + checks + " checks OK");
    }
}
