package dev.rogler.teamwars.core.base;

import dev.rogler.teamwars.core.BaseZone;
import java.util.UUID;

/** No Minecraft stubs: simulate Ender Chest, disconnect and server restarts by skipping calls. */
public final class CoreTransportClockPolicyTests {
    private static int checks;
    private static void yes(boolean ok) { checks++; if (!ok) throw new AssertionError("transport clock check " + checks); }
    public static void main(String[] args) {
        UUID team=UUID.randomUUID(), token=UUID.randomUUID();
        BaseCoreLedger ledger=new BaseCoreLedger(new BaseZone(0,0,0,5000),3600);
        ledger.issue(team,token);
        long start=1_800_000_000_000L;
        var first=CoreTransportClockPolicy.advance(null,token,start);
        yes(first.elapsedSeconds()==0);
        yes(first.checkpoint().token().equals(token));
        // No online carrier and no inventory lookup; the core has been locked in an Ender Chest.
        var chest=CoreTransportClockPolicy.advance(first.checkpoint(),token,start+45_500);
        yes(chest.elapsedSeconds()==45);
        yes(chest.checkpoint().accountedAtMillis()==start+45_000);
        var item=ledger.advanceUnplaced(team,token,chest.elapsedSeconds(),null);
        yes(item.mode()==BaseCoreLedger.Mode.ISSUED && item.carriedSeconds()==45);
        // Fractional seconds must not be lost between accounting calls.
        var next=CoreTransportClockPolicy.advance(chest.checkpoint(),token,start+46_100);
        yes(next.elapsedSeconds()==1);
        yes(next.checkpoint().accountedAtMillis()==start+46_000);
        ledger.advanceUnplaced(team,token,next.elapsedSeconds(),null);
        // Simulate the saved checkpoint across a complete server shutdown of >1 hour.
        var loaded=new CoreTransportClockPolicy.Checkpoint(next.checkpoint().token(),next.checkpoint().accountedAtMillis());
        var offline=CoreTransportClockPolicy.advance(loaded,token,start+7_201_000);
        yes(offline.elapsedSeconds()>=7155);
        var expired=ledger.advanceUnplaced(team,token,offline.elapsedSeconds(),null);
        yes(expired.mode()==BaseCoreLedger.Mode.ESCROW);
        yes(expired.carriedSeconds()==3600);
        yes(!ledger.canPlace(team,token,new BaseCoreLedger.Location("minecraft:overworld",100,64,100)));
        // Token rotation makes both a copied core and its old checkpoint unusable.
        UUID newToken=UUID.randomUUID();
        var recovery=ledger.reissueFromEscrow(team,newToken);
        yes(recovery.carriedSeconds()==3599);
        var fresh=CoreTransportClockPolicy.advance(offline.checkpoint(),newToken,start+7_202_000);
        yes(fresh.elapsedSeconds()==0 && fresh.checkpoint().token().equals(newToken));
        yes(ledger.advanceUnplaced(team,newToken,1,null).mode()==BaseCoreLedger.Mode.ESCROW);
        // Moving the OS clock backwards cannot subtract accounted time.
        var backwards=CoreTransportClockPolicy.advance(first.checkpoint(),token,start-10_000);
        yes(backwards.elapsedSeconds()==0 && backwards.checkpoint().equals(first.checkpoint()));
        // A placed core is not updated by ledger transport accounting.
        UUID t2=UUID.randomUUID(),tok2=UUID.randomUUID();
        ledger.issue(t2,tok2);
        ledger.placed(t2,tok2,new BaseCoreLedger.Location("minecraft:overworld",100,64,100));
        yes(ledger.get(t2).mode()==BaseCoreLedger.Mode.PLACED);
        boolean rejected=false;
        try{ledger.advanceUnplaced(t2,tok2,10_000,null);}catch(IllegalStateException expected){rejected=true;}
        yes(rejected);
        System.out.println("v0.8.6 CoreTransportClockPolicyTests: " + checks + " checks OK");
    }
}
