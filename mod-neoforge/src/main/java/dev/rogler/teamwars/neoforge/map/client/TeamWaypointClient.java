package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import dev.rogler.teamwars.neoforge.map.WaypointSnapshotPayload;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.UUID;

/** Volatile per-login state. Expiry prevents cached team markers surviving a lost server heartbeat. */
public final class TeamWaypointClient {
    private static WaypointSnapshotPayload snapshot;
    private static long acceptedTick=-1, lastSequence=-1;
    private TeamWaypointClient(){}
    public static void receive(WaypointSnapshotPayload packet){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||packet.seq()<=lastSequence)return;
        if(snapshot!=null&&!snapshot.team().equals(packet.team()))TeamWaypointDisplay.clear();
        snapshot=packet;lastSequence=packet.seq();acceptedTick=mc.level.getGameTime();
    }
    public static List<WaypointSyncPolicy.Shared> active(){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||snapshot==null)return List.of();
        long now=mc.level.getGameTime();
        return now>=acceptedTick && now-acceptedTick<=140? snapshot.points():List.of();
    }
    public static UUID team(){
        var mc=Minecraft.getInstance();if(mc.level==null||snapshot==null||mc.level.getGameTime()-acceptedTick>140)
            return WaypointSnapshotPayload.NO_TEAM;
        return snapshot.team();
    }
    public static void clear(){snapshot=null;acceptedTick=-1;lastSequence=-1;TeamWaypointDisplay.clear();}
}
