package dev.rogler.teamwars.core;
import java.util.List;
import java.util.Set;
import java.util.UUID;
public class WaypointSyncPolicyTests {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check " + checks);}
    public static void main(String[] args){
        UUID a=UUID.randomUUID(), b=UUID.randomUUID(), enemy=UUID.randomUUID(), team=UUID.randomUUID(), other=UUID.randomUUID();
        var point=new WaypointSyncPolicy.Point("minecraft:overworld",-40,67,88,"Mine",0x005599);
        var s=new WaypointSyncPolicy.Shared(UUID.randomUUID(),team,a,point);
        check(WaypointSyncPolicy.canSee(s,a,team,Set.of(a,b)));
        check(WaypointSyncPolicy.canSee(s,b,team,Set.of(a,b)));
        check(!WaypointSyncPolicy.canSee(s,enemy,team,Set.of(a,b)));
        check(!WaypointSyncPolicy.canSee(s,b,other,Set.of(a,b)));
        check(!WaypointSyncPolicy.canSee(s,b,team,Set.of(b))); // owner departed => revoke
        check(!WaypointSyncPolicy.canRevoke(s,b,team,Set.of(a,b)));
        check(WaypointSyncPolicy.canRevoke(s,a,team,Set.of(a,b)));
        check(WaypointSyncPolicy.visible(List.of(s),b,team,Set.of(a,b)).size()==1);
        check(WaypointSyncPolicy.visible(List.of(s),b,team,Set.of(b)).isEmpty());
        check(WaypointSyncPolicy.missing(List.of(point,point),List.of()).size()==1);
        check(WaypointSyncPolicy.missing(List.of(point),List.of(point)).isEmpty());
        try{new WaypointSyncPolicy.Point("minecraft:overworld",0,64,0,"\nleak",0);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
        try{new WaypointSyncPolicy.Point("minecraft:overworld",30_000_001,64,0,"ok",0);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
        try{new WaypointSyncPolicy.Point("minecraft:overworld",0,64,0,"ok",-1);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
        System.out.println("v0.7.6 WaypointSyncPolicyTests: " + checks + " checks OK");
    }
}
