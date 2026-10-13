package dev.rogler.teamwars.core.base;

public final class CoreManualTradePolicyTests {
    public static void main(String[] args){
        int n=0;
        check(CoreManualTradePolicy.mayTrade(true,false,1,true));n++;
        check(CoreManualTradePolicy.mayTrade(true,false,64,true));n++;
        check(!CoreManualTradePolicy.mayTrade(true,false,0,true));n++;
        check(!CoreManualTradePolicy.mayTrade(false,false,1,true));n++;
        check(!CoreManualTradePolicy.mayTrade(true,true,1,true));n++;
        check(!CoreManualTradePolicy.mayTrade(true,false,1,false));n++;
        check(!CoreManualTradePolicy.mayTrade(false,true,0,false));n++;
        System.out.println("v0.5.4 CoreManualTradePolicyTests: "+n+" checks OK");
    }
    private static void check(boolean ok){if(!ok)throw new AssertionError("manual book payment invariant failed");}
}
