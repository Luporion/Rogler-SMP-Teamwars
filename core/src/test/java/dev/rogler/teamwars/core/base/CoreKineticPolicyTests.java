package dev.rogler.teamwars.core.base;
public final class CoreKineticPolicyTests {
    public static void main(String[] args) {
        int n=0;
        check(CoreKineticPolicy.STRESS_UNITS==256);n++;
        check(CoreKineticPolicy.RPM==16);n++;
        check(CoreKineticPolicy.canUpgrade(0,16,8,true,false,true));n++;
        check(!CoreKineticPolicy.canUpgrade(0,15,8,true,false,true));n++;
        check(!CoreKineticPolicy.canUpgrade(0,16,7,true,false,true));n++;
        check(!CoreKineticPolicy.canUpgrade(1,16,8,true,false,true));n++;
        check(!CoreKineticPolicy.canUpgrade(0,16,8,false,false,true));n++;
        check(!CoreKineticPolicy.canUpgrade(0,16,8,true,true,true));n++;
        check(!CoreKineticPolicy.canUpgrade(0,16,8,true,false,false));n++;
        System.out.println("v0.5.2 CoreKineticPolicyTests: "+n+" checks OK");
    }
    private static void check(boolean pass){if(!pass)throw new AssertionError("Core kinetic invariant violated");}
}
