package dev.rogler.teamwars.core.base;
import java.util.UUID;
public final class RaidPolicyTests {
    static int n;
    static void yes(boolean b){n++;if(!b)throw new AssertionError("raid check "+n);}
    public static void main(String[] args){
        UUID defender=UUID.randomUUID(), attacker=UUID.randomUUID(), third=UUID.randomUUID();
        long now=100000;
        var r=new RaidPolicy.Window(defender,null,now+60000,RaidPolicy.CORE_HP,0);
        yes(RaidPolicy.eligible(r,defender,attacker,now,0,true));
        yes(!RaidPolicy.eligible(r,defender,defender,now,0,true));
        yes(!RaidPolicy.eligible(r,defender,attacker,now,now+5000,true));
        yes(!RaidPolicy.eligible(r,defender,attacker,now,0,false));
        yes(!RaidPolicy.eligible(r,defender,attacker,now+60000,0,true));
        r=RaidPolicy.hit(r,attacker,now);
        yes(r.hp()==9);yes(r.attacker().equals(attacker));
        yes(!RaidPolicy.eligible(r,defender,third,now+1000,0,true));
        try {RaidPolicy.hit(r,third,now+1000);throw new AssertionError();}catch(IllegalStateException ok){n++;}
        try {RaidPolicy.hit(r,attacker,now+100);throw new AssertionError();}catch(IllegalStateException ok){n++;}
        for(int i=0;i<9;i++)r=RaidPolicy.hit(r,attacker,now+(i+1)*800);
        yes(r.hp()==0);
        yes(!RaidPolicy.eligible(r,defender,attacker,now+9000,0,true));
        var ledger=new BaseCoreLedger(new dev.rogler.teamwars.core.BaseZone(0,0,0,5000),3600);
        UUID token=UUID.randomUUID();var loc=new BaseCoreLedger.Location("minecraft:overworld",200,70,200);
        ledger.issue(defender,token);ledger.placed(defender,token,loc);
        var after=ledger.defeatPlaced(defender,token,loc);
        yes(after.mode()==BaseCoreLedger.Mode.ESCROW);yes(after.carriedSeconds()==0);
        try {ledger.defeatPlaced(defender,token,loc);throw new AssertionError();}catch(IllegalStateException ok){n++;}
        UUID fresh=UUID.randomUUID();ledger.reissueFromEscrow(defender,fresh);
        yes(!ledger.canPlace(defender,token,loc));yes(ledger.canPlace(defender,fresh,loc));
        System.out.println("v0.4.0 RaidPolicyTests: "+n+" checks OK");
    }
}
