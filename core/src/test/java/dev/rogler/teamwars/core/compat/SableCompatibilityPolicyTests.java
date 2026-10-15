package dev.rogler.teamwars.core.compat;

import java.util.List;

public final class SableCompatibilityPolicyTests {
    private static int checks;
    private static void ok(boolean b){checks++;if(!b)throw new AssertionError("Sable compatibility check "+checks);}
    public static void main(String[] ignored) {
        var absent=List.<SableCompatibilityPolicy.Mod>of();
        var sable=new SableCompatibilityPolicy.Mod("sable","Sable","2.0.6");
        var dv=new SableCompatibilityPolicy.Mod("sabledestructive","Sable: Destructive","2.2.0");
        var collision=new SableCompatibilityPolicy.Mod("unknown_collisiondamage","Sable: Collision Damage","1.2.0");
        var pair=List.of(sable,dv);
        ok(SableCompatibilityPolicy.assess(absent,SableSplitConfigAudit.State.MISSING).status().equals("ABSENT"));
        ok(SableCompatibilityPolicy.assess(pair,SableSplitConfigAudit.State.ENABLED).status().equals("READY_TO_TEST"));
        ok(!SableCompatibilityPolicy.assess(pair,SableSplitConfigAudit.State.ENABLED).warning());
        ok(SableCompatibilityPolicy.assess(pair,SableSplitConfigAudit.State.DISABLED).warning());
        ok(SableCompatibilityPolicy.assess(pair,SableSplitConfigAudit.State.INVALID).warning());
        ok(SableCompatibilityPolicy.assess(pair,SableSplitConfigAudit.State.AMBIGUOUS).warning());
        ok(SableCompatibilityPolicy.assess(pair,SableSplitConfigAudit.State.MISSING).warning());
        ok(SableCompatibilityPolicy.assess(List.of(sable),SableSplitConfigAudit.State.ENABLED).warning());
        ok(SableCompatibilityPolicy.assess(List.of(dv),SableSplitConfigAudit.State.ENABLED).warning());
        ok(SableCompatibilityPolicy.assess(List.of(sable,dv,collision),SableSplitConfigAudit.State.ENABLED).status().equals("A/B_TEST"));
        ok(SableCompatibilityPolicy.atLeast("2.2.0",2,2,0));
        ok(SableCompatibilityPolicy.atLeast("2.2.1",2,2,0));
        ok(SableCompatibilityPolicy.atLeast("3.0.0",2,2,0));
        ok(!SableCompatibilityPolicy.atLeast("2.1.9",2,2,0));
        ok(!SableCompatibilityPolicy.atLeast("bad",2,2,0));
        ok(!SableCompatibilityPolicy.atLeast("1.99.99",2,2,0));
        ok(SableCompatibilityPolicy.atLeast("v2.0.6+release",2,0,3));
        ok(SableCompatibilityPolicy.exact(pair,"sabledestructive")!=null);
        ok(SableCompatibilityPolicy.exact(pair,"wrong")==null);
        ok(SableCompatibilityPolicy.byName(pair,"destructive")!=null);
        ok(SableCompatibilityPolicy.assess(List.of(new SableCompatibilityPolicy.Mod("sable","Sable","2.0.2"),dv),SableSplitConfigAudit.State.ENABLED).warning());
        ok(SableCompatibilityPolicy.assess(List.of(sable,new SableCompatibilityPolicy.Mod("sabledestructive","Sable: Destructive","2.1.9")),SableSplitConfigAudit.State.ENABLED).warning());
        System.out.println("v0.6.3 SableCompatibilityPolicyTests: "+checks+" checks OK");
    }
}
