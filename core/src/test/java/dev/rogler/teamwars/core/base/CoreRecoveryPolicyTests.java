package dev.rogler.teamwars.core.base;

import java.util.UUID;

public class CoreRecoveryPolicyTests {
    static int checks;
    static void eq(CoreRecoveryPolicy.Decision got,CoreRecoveryPolicy.Decision want) {
        checks++;if(got!=want)throw new AssertionError("Recovery expected "+want+" got "+got);
    }
    public static void main(String[] args) {
        var p=new CoreRecoveryPolicy();UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        eq(p.missingAir(a,10000),CoreRecoveryPolicy.Decision.WAIT);
        eq(p.missingAir(a,14999),CoreRecoveryPolicy.Decision.WAIT);
        eq(p.missingAir(a,15000),CoreRecoveryPolicy.Decision.RESTORE);
        p.healthy(a,15001);
        eq(p.missingAir(a,15002),CoreRecoveryPolicy.Decision.WAIT);
        eq(p.missingAir(a,20002),CoreRecoveryPolicy.Decision.RESTORE);
        eq(p.missingAir(a,20003),CoreRecoveryPolicy.Decision.QUARANTINE);
        p.resolved(a);
        eq(p.missingAir(a,21000),CoreRecoveryPolicy.Decision.WAIT);
        eq(p.missingAir(b,22000),CoreRecoveryPolicy.Decision.WAIT);
        p.unloaded(b);
        eq(p.missingAir(b,28000),CoreRecoveryPolicy.Decision.WAIT);
        eq(p.missingAir(b,33000),CoreRecoveryPolicy.Decision.RESTORE);
        p.healthy(b,153001);
        eq(p.missingAir(b,154000),CoreRecoveryPolicy.Decision.WAIT);
        eq(p.missingAir(b,159000),CoreRecoveryPolicy.Decision.RESTORE);
        var c=UUID.randomUUID();
        eq(p.missingAir(c,1000),CoreRecoveryPolicy.Decision.WAIT);
        eq(p.missingAir(c,0),CoreRecoveryPolicy.Decision.WAIT);
        System.out.println("v0.6.2 CoreRecoveryPolicyTests: "+checks+" checks OK");
    }
}
