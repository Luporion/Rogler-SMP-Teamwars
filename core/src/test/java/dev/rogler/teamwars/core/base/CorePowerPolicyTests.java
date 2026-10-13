package dev.rogler.teamwars.core.base;

public final class CorePowerPolicyTests {
    private static int checks;
    private static void eq(int expected, int actual) { assert expected == actual: expected+" != "+actual; checks++; }
    private static void yes(boolean actual) { assert actual; checks++; }
    private static void no(boolean actual) { assert !actual; checks++; }
    public static void main(String[] args) {
        eq(27,CorePowerPolicy.SLOTS);
        eq(20,CorePowerPolicy.generationPerTick(0));
        eq(40,CorePowerPolicy.generationPerTick(1));
        eq(80,CorePowerPolicy.generationPerTick(2));
        eq(100,CorePowerPolicy.generate(80,0,true));
        eq(80,CorePowerPolicy.generate(80,0,false));
        eq(20_000,CorePowerPolicy.generate(19_990,2,true));
        eq(0,CorePowerPolicy.clampEnergy(-100));
        eq(20_000,CorePowerPolicy.clampEnergy(1_000_000));
        eq(160,CorePowerPolicy.extractable(999,500));
        eq(2,CorePowerPolicy.extractable(2,500));
        eq(0,CorePowerPolicy.extractable(0,500));
        eq(0,CorePowerPolicy.extractable(100,-5));
        yes(CorePowerPolicy.canUpgrade(0,16,8,0,true,false));
        yes(CorePowerPolicy.canUpgrade(1,32,0,8,true,false));
        no(CorePowerPolicy.canUpgrade(0,15,8,0,true,false));
        no(CorePowerPolicy.canUpgrade(1,32,0,7,true,false));
        no(CorePowerPolicy.canUpgrade(1,32,0,8,false,false));
        no(CorePowerPolicy.canUpgrade(1,32,0,8,true,true));
        no(CorePowerPolicy.canUpgrade(2,99,99,99,true,false));
        eq(0,CoreIntegrationPolicy.proposedStressCapacity(0));
        eq(256,CoreIntegrationPolicy.proposedStressCapacity(1));
        eq(512,CoreIntegrationPolicy.proposedStressCapacity(2));
        no(CoreIntegrationPolicy.itemAutomationAllowed(false));
        yes(CoreIntegrationPolicy.itemAutomationAllowed(true));
        System.out.println("v0.5.1 CorePowerPolicyTests: "+checks+" checks OK");
    }
}
