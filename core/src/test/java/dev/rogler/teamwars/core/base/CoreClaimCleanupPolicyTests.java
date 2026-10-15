package dev.rogler.teamwars.core.base;

public class CoreClaimCleanupPolicyTests {
    public static void main(String[] args){
        int n=0;
        for(boolean exists:new boolean[]{false,true})for(boolean owned:new boolean[]{false,true}){
            var action=CoreClaimCleanupPolicy.decide(exists,owned);
            boolean safe=(action==CoreClaimCleanupPolicy.Action.UNCLAIM_OWN)==(exists&&owned);
            if(!safe)throw new AssertionError("claim ownership cleanup");n++;
        }
        System.out.println("v0.6.2 CoreClaimCleanupPolicyTests: "+n+" checks OK");
    }
}
