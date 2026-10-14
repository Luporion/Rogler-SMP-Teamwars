package dev.rogler.teamwars.core.base;

public class CoreIntegrityPolicyTests {
  private static int checks;
  private static void eq(CoreIntegrityPolicy.Action actual,CoreIntegrityPolicy.Action expected){
    checks++; if(actual!=expected)throw new AssertionError("Core integrity expected "+expected+", got "+actual);
  }
  public static void main(String[] args){
    var A=CoreIntegrityPolicy.Action.class;
    eq(CoreIntegrityPolicy.decide(true,true,true,false,true),CoreIntegrityPolicy.Action.HEALTHY);
    eq(CoreIntegrityPolicy.decide(true,false,false,true,true),CoreIntegrityPolicy.Action.DEFER_UNLOADED);
    eq(CoreIntegrityPolicy.decide(true,true,false,true,true),CoreIntegrityPolicy.Action.RESTORE_AIR);
    eq(CoreIntegrityPolicy.decide(true,true,false,false,true),CoreIntegrityPolicy.Action.ESCROW_RELEASE);
    eq(CoreIntegrityPolicy.decide(true,true,false,false,false),CoreIntegrityPolicy.Action.ESCROW_RELEASE);
    eq(CoreIntegrityPolicy.decide(false,true,false,true,true),CoreIntegrityPolicy.Action.RELEASE_STALE_CLAIMS);
    eq(CoreIntegrityPolicy.decide(false,false,false,false,true),CoreIntegrityPolicy.Action.RELEASE_STALE_CLAIMS);
    eq(CoreIntegrityPolicy.decide(false,false,false,false,false),CoreIntegrityPolicy.Action.HEALTHY);
    eq(CoreIntegrityPolicy.decide(false,true,true,false,true),CoreIntegrityPolicy.Action.RELEASE_STALE_CLAIMS);
    System.out.println("v0.6.0 CoreIntegrityPolicyTests: "+checks+" checks OK");
  }
}
