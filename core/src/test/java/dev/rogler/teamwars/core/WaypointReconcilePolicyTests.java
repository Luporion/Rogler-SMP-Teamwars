package dev.rogler.teamwars.core;

import java.util.List;

public final class WaypointReconcilePolicyTests {
    private static int n;
    private static void check(boolean value) { n++; if(!value)throw new AssertionError("check "+n); }
    private static WaypointSyncPolicy.Point p(int x,String name,int color) {
        return new WaypointSyncPolicy.Point("minecraft:overworld",x,70,25,name,color);
    }
    public static void main(String[] args) {
        var mine=p(10,"Mine",0xffffff); var same=p(10,"Mine",0xffffff);
        var named=p(10,"Renamed",0xff0000); var moved=p(20,"Mine",0xffffff);
        check(WaypointReconcilePolicy.compare(List.of(mine),List.of(same)).same()==1);
        check(WaypointReconcilePolicy.compare(List.of(mine),List.of(named)).metadata()==1);
        check(WaypointReconcilePolicy.compare(List.of(mine),List.of(moved)).possibleMoves()==2);
        check(WaypointReconcilePolicy.compare(List.of(mine,mine),List.of()).ambiguous()==1);
        check(WaypointReconcilePolicy.compare(List.of(mine),List.of()).ftbOnly()==1);
        check(WaypointReconcilePolicy.compare(List.of(),List.of(mine)).xaeroOnly()==1);
        check(WaypointReconcilePolicy.safeMissing(List.of(mine,mine),List.of()).isEmpty());
        check(WaypointReconcilePolicy.safeMissing(List.of(mine),List.of(moved)).isEmpty());
        check(WaypointReconcilePolicy.safeMissing(List.of(mine),List.of(named)).isEmpty());
        check(WaypointReconcilePolicy.safeMissing(List.of(mine),List.of()).equals(List.of(mine)));
        check(WaypointReconcilePolicy.safeMissing(List.of(mine),List.of(same)).isEmpty());
        check(!WaypointReconcilePolicy.canReplaceMetadata(List.of(mine,mine),List.of(named),mine));
        check(WaypointReconcilePolicy.canReplaceMetadata(List.of(mine),List.of(named),mine));
        check(!WaypointReconcilePolicy.canReplaceMetadata(List.of(mine),List.of(moved),mine));
        String tok=WaypointReconcilePolicy.token("ftb",mine,named);
        check(tok.length()==16);
        check(tok.equals(WaypointReconcilePolicy.token("ftb",mine,named)));
        check(!tok.equals(WaypointReconcilePolicy.token("xaero",mine,named)));
        check(!tok.equals(WaypointReconcilePolicy.token("ftb",named,mine)));
        check(!tok.equals(WaypointReconcilePolicy.token("ftb",mine,p(10,"New name",0xffffff))));
        try {WaypointReconcilePolicy.token("bad",mine,named);throw new AssertionError();}catch(IllegalArgumentException e){n++;}
        try {WaypointReconcilePolicy.token("ftb",mine,moved);throw new AssertionError();}catch(IllegalArgumentException e){n++;}
        check(WaypointReconcilePolicy.compare(List.of(mine),List.of(p(10,"Mine",0))).metadata()==1);
        // Enforce dimension-scoped comparison; no overwrites between Overworld and Nether.
        var nether=new WaypointSyncPolicy.Point("minecraft:the_nether",10,70,25,"Mine",0xffffff);
        check(WaypointReconcilePolicy.compare(List.of(mine),List.of(nether)).ftbOnly()==1);
        check(WaypointReconcilePolicy.compare(List.of(mine),List.of(nether)).xaeroOnly()==1);
        check(WaypointReconcilePolicy.safeMissing(List.of(nether),List.of(mine)).equals(List.of(nether)));
        check(WaypointReconcilePolicy.canReplaceMetadata(List.of(mine),List.of(named),mine));
        check(!WaypointReconcilePolicy.canReplaceMetadata(List.of(mine,p(40,"Mine",0xffffff)),List.of(named),mine));
        System.out.println("v0.7.7 WaypointReconcilePolicyTests: "+n+" checks OK");
    }
}
