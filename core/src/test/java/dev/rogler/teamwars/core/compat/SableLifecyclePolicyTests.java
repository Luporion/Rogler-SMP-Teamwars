package dev.rogler.teamwars.core.compat;

public final class SableLifecyclePolicyTests {
    private static int checks;
    private static void eq(Object a,Object b){checks++;if(!java.util.Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        // Explicit enum constants instead of reflecting on Class.
        eq(SableLifecyclePolicy.eligibility(true,"2.0.6",false),SableLifecyclePolicy.Reason.ACTIVE);
        eq(SableLifecyclePolicy.eligibility(false,"2.0.6",false),SableLifecyclePolicy.Reason.OFF);
        eq(SableLifecyclePolicy.eligibility(true,"2.0.6",true),SableLifecyclePolicy.Reason.DESTRUCTIVE_PRESENT);
        eq(SableLifecyclePolicy.eligibility(true,null,false),SableLifecyclePolicy.Reason.SABLE_ABSENT);
        eq(SableLifecyclePolicy.eligibility(true,"",false),SableLifecyclePolicy.Reason.SABLE_ABSENT);
        eq(SableLifecyclePolicy.eligibility(true,"2.0.7",false),SableLifecyclePolicy.Reason.VERSION_NOT_VERIFIED);
        eq(SableLifecyclePolicy.eligibility(true,"2.0.6+unknown",false),SableLifecyclePolicy.Reason.VERSION_NOT_VERIFIED);
        eq(SableLifecyclePolicy.eligibility(true,"1.0.6",false),SableLifecyclePolicy.Reason.VERSION_NOT_VERIFIED);
        eq(SableLifecyclePolicy.skipTick(true),true);
        eq(SableLifecyclePolicy.skipTick(false),false);
        eq(SableLifecyclePolicy.skipSplit(true,false),true);
        eq(SableLifecyclePolicy.skipSplit(false,true),true);
        eq(SableLifecyclePolicy.skipSplit(true,true),true);
        eq(SableLifecyclePolicy.skipSplit(false,false),false);
        eq(SableLifecyclePolicy.assemblesAnyGroup(0,0,0),false);
        eq(SableLifecyclePolicy.assemblesAnyGroup(1,25,25),false);
        eq(SableLifecyclePolicy.assemblesAnyGroup(1,24,25),true);
        eq(SableLifecyclePolicy.assemblesAnyGroup(2,25,25),true);
        eq(SableLifecyclePolicy.retainLargest(1,25,25),false);
        eq(SableLifecyclePolicy.retainLargest(2,25,25),true);
        eq(SableLifecyclePolicy.retainLargest(2,24,25),false);
        eq(SableLifecyclePolicy.retainLargest(0,0,0),false);
        System.out.println("v0.6.4 SableLifecyclePolicyTests: "+checks+" checks OK");
    }
}
