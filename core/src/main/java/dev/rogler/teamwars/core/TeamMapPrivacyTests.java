package dev.rogler.teamwars.core;
import java.util.*;
/** Pure offline regression tests for map disclosure and travel provenance. */
public final class TeamMapPrivacyTests {
 private static int checks;
 private static void test(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
 public static void main(String[] args) {
  UUID a=UUID.randomUUID(), b=UUID.randomUUID(), c=UUID.randomUUID(), red=UUID.randomUUID(), blue=UUID.randomUUID();
  test(TeamMapPrivacyPolicy.sharePosition(a,a,null,null),"own position");
  test(TeamMapPrivacyPolicy.sharePosition(a,b,red,red),"same FTB party");
  test(!TeamMapPrivacyPolicy.sharePosition(a,c,red,blue),"enemies hidden");
  test(!TeamMapPrivacyPolicy.sharePosition(a,b,null,null),"two solo players hidden");
  test(!TeamMapPrivacyPolicy.sharePosition(a,b,red,null),"unavailable membership hidden");
  test(TeamMapPrivacyPolicy.shareCore(red,red),"own core visible");
  test(!TeamMapPrivacyPolicy.shareCore(blue,red),"enemy core hidden");
  test(!TeamMapPrivacyPolicy.shareCore(null,red),"unassigned core hidden");
  ExplorationIndex index = new ExplorationIndex();
  ExplorationTrail trail = new ExplorationTrail(index,8);
  ChunkKey p0=new ChunkKey("minecraft:overworld",-2,2);
  test(trail.sample(a,p0)==1,"personal explore");
  test(trail.sample(a,new ChunkKey("minecraft:overworld",2,2))==4,"interpolate nearby movement");
  test(index.personal(a).size()==5,"no repeat");
  test(trail.sample(a,new ChunkKey("minecraft:overworld",900,900))==1,"teleport endpoint only");
  test(!index.personal(a).contains(new ChunkKey("minecraft:overworld",3,3)),"teleport cannot reveal route");
  test(trail.sample(a,new ChunkKey("minecraft:the_nether",0,0))==1,"dimension boundary");
  trail.logout(a);
  test(trail.sample(a,p0)==0,"personal discovery persists across logout");
  index.discover(b,new ChunkKey("minecraft:overworld",50,50));
  test(index.visibleTo(Set.of(a,b)).size()==index.personal(a).size()+1,"union with teammate");
  test(index.delta(Set.of(a,b),Set.of(a)).revoked().contains(new ChunkKey("minecraft:overworld",50,50)),"team leave revokes B only");
  test(!index.delta(Set.of(a,b),Set.of(a)).revoked().contains(p0),"A retains own map");
  test(TeamMapPrivacyPolicy.sharePosition(a,b,red,red),"tracked before team leave");
  test(!TeamMapPrivacyPolicy.sharePosition(a,b,red,blue),"tracked removed on team switch");
  System.out.println("TeamMapPrivacyTests: " + checks + " checks OK");
 }
}
