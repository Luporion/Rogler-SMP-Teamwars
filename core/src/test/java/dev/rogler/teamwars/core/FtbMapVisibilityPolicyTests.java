package dev.rogler.teamwars.core;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FtbMapVisibilityPolicyTests {
  public static void main(String[] args) {
    UUID a=UUID.randomUUID(), b=UUID.randomUUID(), e=UUID.randomUUID();
    UUID team=UUID.randomUUID(), enemy=UUID.randomUUID();
    var own=new TeamAssignmentPolicy.Party(team,"a",Set.of(a,b));
    var hostile=new TeamAssignmentPolicy.Party(enemy,"e",Set.of(e));
    var roster=Map.of(a,own,b,own,e,hostile);
    check(FtbMapVisibilityPolicy.visible(a,List.of(a,b,e),roster).equals(List.of(a,b)),"not teammates only");
    check(FtbMapVisibilityPolicy.visible(e,List.of(a,b,e),roster).equals(List.of(e)),"op/enemy visibility");
    check(FtbMapVisibilityPolicy.visible(a,List.of(a,b,e),Map.of(a,own,b,hostile,e,hostile)).equals(List.of(a)),"stale assignment");
    check(FtbMapVisibilityPolicy.visible(a,List.of(a,b,e),Map.of()).equals(List.of(a)),"missing FTB must fail closed");
    check(FtbMapVisibilityPolicy.visible(a,List.of(a,a,b,e),roster).equals(List.of(a,b)),"duplicate candidates");
    System.out.println("v0.7.3 FtbMapVisibilityPolicyTests: 5 checks OK");
  }
  private static void check(boolean yes, String name) {if(!yes)throw new AssertionError(name);}
}
