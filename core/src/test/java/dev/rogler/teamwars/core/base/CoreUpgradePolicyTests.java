package dev.rogler.teamwars.core.base;
public final class CoreUpgradePolicyTests {
    private static int checks=0;
    private static void ok(boolean v) { checks++; if(!v) throw new AssertionError("upgrade test " + checks); }
    public static void main(String[] args) {
        ok(CoreUpgradePolicy.maxHealth(0)==10);
        ok(CoreUpgradePolicy.maxHealth(1)==15);
        ok(CoreUpgradePolicy.maxHealth(2)==20);
        ok(CoreUpgradePolicy.diamondsForNext(0)==8);
        ok(CoreUpgradePolicy.diamondsForNext(1)==16);
        ok(CoreUpgradePolicy.netheriteForNext(0)==0);
        ok(CoreUpgradePolicy.netheriteForNext(1)==1);
        ok(CoreUpgradePolicy.canBuy(0,8,0,true,false));
        ok(!CoreUpgradePolicy.canBuy(0,7,0,true,false));
        ok(!CoreUpgradePolicy.canBuy(1,16,0,true,false));
        ok(CoreUpgradePolicy.canBuy(1,16,1,true,false));
        ok(!CoreUpgradePolicy.canBuy(1,16,1,false,false));
        ok(!CoreUpgradePolicy.canBuy(1,16,1,true,true));
        ok(!CoreUpgradePolicy.canBuy(2,200,50,true,false));
        try { CoreUpgradePolicy.maxHealth(3); throw new AssertionError(); }
        catch(IllegalArgumentException expected) { checks++; }
        System.out.println("v0.5.0 CoreUpgradePolicyTests: "+checks+" checks OK");
    }
}
