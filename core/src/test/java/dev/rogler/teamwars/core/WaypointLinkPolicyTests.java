package dev.rogler.teamwars.core;

import java.util.List;
import java.util.UUID;

public final class WaypointLinkPolicyTests {
    private static int checks;
    private static void ok(boolean good) {checks++;if(!good)throw new AssertionError("link check #"+checks);}
    private static WaypointSyncPolicy.Point p(int x,String n,int c) {return new WaypointSyncPolicy.Point("minecraft:overworld",x,64,-9,n,c);}
    private static WaypointLinkPolicy.Review one(WaypointLinkPolicy.Link l,List<WaypointSyncPolicy.Point> f,List<WaypointSyncPolicy.Point> x){
        return WaypointLinkPolicy.review(List.of(l),f,x).get(0);
    }
    public static void main(String[] args) {
        var base=p(20,"Home",0x112233);var renamed=p(20,"Base",0x112233);var changed=p(20,"Home",0xffffff);
        var link=new WaypointLinkPolicy.Link(UUID.randomUUID(),base,base);
        ok(WaypointLinkPolicy.canLink(List.of(),base,base,List.of(base),List.of(base)));
        ok(!WaypointLinkPolicy.canLink(List.of(link),base,base,List.of(base),List.of(base)));
        ok(!WaypointLinkPolicy.canLink(List.of(),base,p(23,"Home",0),List.of(base),List.of()));
        ok(!WaypointLinkPolicy.canLink(List.of(),base,base,List.of(base,base),List.of(base)));
        ok(!WaypointLinkPolicy.canLink(List.of(),base,base,List.of(base),List.of(base,base)));
        ok(one(link,List.of(base),List.of(base)).state()==WaypointLinkPolicy.State.UNCHANGED);
        var f=one(link,List.of(renamed),List.of(base));
        ok(f.state()==WaypointLinkPolicy.State.FTB_METADATA_CHANGED);
        ok(WaypointLinkPolicy.safeToApply(f,f.token()));
        ok(!WaypointLinkPolicy.safeToApply(f,"wrong"));
        ok(f.token().length()==20);
        ok(!f.token().equals(one(link,List.of(changed),List.of(base)).token()));
        ok(WaypointLinkPolicy.committed(f).ftbBaseline().equals(renamed));
        ok(WaypointLinkPolicy.committed(f).xaeroBaseline().equals(renamed));
        var x=one(link,List.of(base),List.of(changed));
        ok(x.state()==WaypointLinkPolicy.State.XAERO_METADATA_CHANGED);
        ok(WaypointLinkPolicy.committed(x).ftbBaseline().equals(changed));
        ok(one(link,List.of(changed),List.of(renamed)).state()==WaypointLinkPolicy.State.CONFLICT);
        ok(!one(link,List.of(changed),List.of(renamed)).canApply());
        ok(one(link,List.of(changed),List.of(changed)).state()==WaypointLinkPolicy.State.CONVERGED);
        ok(one(link,List.of(),List.of(base)).state()==WaypointLinkPolicy.State.MISSING_FTB);
        ok(one(link,List.of(base),List.of()).state()==WaypointLinkPolicy.State.MISSING_XAERO);
        ok(one(link,List.of(),List.of()).state()==WaypointLinkPolicy.State.MISSING_BOTH);
        ok(one(link,List.of(base,base),List.of(base)).state()==WaypointLinkPolicy.State.AMBIGUOUS);
        ok(one(link,List.of(p(99,"Home",0)),List.of(base)).state()==WaypointLinkPolicy.State.MISSING_FTB);
        ok(!one(link,List.of(p(99,"Home",0)),List.of(base)).canApply());
        var nether=new WaypointSyncPolicy.Point("minecraft:the_nether",20,64,-9,"Home",0x112233);
        ok(one(link,List.of(nether),List.of(base)).state()==WaypointLinkPolicy.State.MISSING_FTB);
        ok(one(link,List.of(renamed),List.of(base)).token().equals(f.token()));
        try {new WaypointLinkPolicy.Link(UUID.randomUUID(),base,p(30,"different",5)); throw new AssertionError("must reject");}
        catch(IllegalArgumentException expected){checks++;}
        try {WaypointLinkPolicy.committed(one(link,List.of(),List.of()));throw new AssertionError("must reject");}
        catch(IllegalArgumentException expected){checks++;}
        ok(WaypointLinkPolicy.review(List.of(),List.of(),List.of()).isEmpty());
        var missingF=one(link,List.of(),List.of(base));
        var missingX=one(link,List.of(base),List.of());
        ok(WaypointLinkPolicy.safeToDelete(missingF,missingF.token()));
        ok(WaypointLinkPolicy.safeToDelete(missingX,missingX.token()));
        ok(!WaypointLinkPolicy.safeToDelete(missingF,"old"));
        ok(!WaypointLinkPolicy.safeToDelete(one(link,List.of(),List.of(renamed)),missingF.token()));
        ok(!WaypointLinkPolicy.safeToDelete(one(link,List.of(),List.of()),one(link,List.of(),List.of()).token()));
        var moved=p(40,"Home",0x112233);
        ok(WaypointLinkPolicy.safeToMove(missingF,missingF.token(),"ftb",moved,
            List.of(moved),List.of(base),List.of(link)));
        ok(WaypointLinkPolicy.safeToMove(missingX,missingX.token(),"xaero",moved,
            List.of(base),List.of(moved),List.of(link)));
        ok(!WaypointLinkPolicy.safeToMove(missingF,"old","ftb",moved,
            List.of(moved),List.of(base),List.of(link)));
        ok(!WaypointLinkPolicy.safeToMove(missingF,missingF.token(),"ftb",p(40,"Other",0x112233),
            List.of(p(40,"Other",0x112233)),List.of(base),List.of(link)));
        ok(!WaypointLinkPolicy.safeToMove(missingF,missingF.token(),"ftb",moved,
            List.of(moved),List.of(base,moved),List.of(link)));
        ok(!WaypointLinkPolicy.safeToMove(missingF,missingF.token(),"ftb",moved,
            List.of(moved,moved),List.of(base),List.of(link)));
        ok(!WaypointLinkPolicy.safeToMove(one(link,List.of(changed),List.of(base)),
            one(link,List.of(changed),List.of(base)).token(),"ftb",moved,
            List.of(moved,changed),List.of(base),List.of(link)));
        var m=WaypointLinkPolicy.moved(link,moved,moved);
        ok(m.id().equals(link.id())&&m.locationKey().equals(moved.locationKey()));
        try{WaypointLinkPolicy.moved(link,base,moved);throw new AssertionError("must reject");}
        catch(IllegalArgumentException expected){checks++;}
        System.out.println("v0.8.1 WaypointLinkPolicyTests: "+checks+" checks OK");
    }
}
