package dev.rogler.teamwars.neoforge.map;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Private, server-authorized state replacement; never global broadcast. */
public record WaypointSnapshotPayload(long seq,UUID team,List<WaypointSyncPolicy.Shared> points) implements CustomPacketPayload {
    public static final UUID NO_TEAM=new UUID(0L,0L);
    public static final Type<WaypointSnapshotPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"waypoint_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WaypointSnapshotPayload> STREAM_CODEC=StreamCodec.of(WaypointSnapshotPayload::write,WaypointSnapshotPayload::read);
    public WaypointSnapshotPayload{
        if(team==null||points==null||points.size()>WaypointSyncPolicy.MAX_TEAM_WAYPOINTS)throw new IllegalArgumentException("Invalid team waypoint snapshot");
        points=List.copyOf(points);
    }
    static void writePoint(RegistryFriendlyByteBuf b,WaypointSyncPolicy.Point p){
        b.writeUtf(p.dimension(),WaypointSyncPolicy.MAX_DIMENSION);b.writeInt(p.x());b.writeInt(p.y());b.writeInt(p.z());
        b.writeUtf(p.name(),WaypointSyncPolicy.MAX_NAME);b.writeInt(p.color());
    }
    static WaypointSyncPolicy.Point readPoint(RegistryFriendlyByteBuf b){
        return new WaypointSyncPolicy.Point(b.readUtf(WaypointSyncPolicy.MAX_DIMENSION),
            b.readInt(),b.readInt(),b.readInt(),b.readUtf(WaypointSyncPolicy.MAX_NAME),b.readInt());
    }
    private static void write(RegistryFriendlyByteBuf b,WaypointSnapshotPayload p){
        b.writeLong(p.seq());b.writeUUID(p.team());b.writeVarInt(p.points().size());
        for(var e:p.points()){b.writeUUID(e.id());b.writeUUID(e.owner());writePoint(b,e.point());}
    }
    private static WaypointSnapshotPayload read(RegistryFriendlyByteBuf b){
        long seq=b.readLong();UUID team=b.readUUID();int n=b.readVarInt();
        if(n<0||n>WaypointSyncPolicy.MAX_TEAM_WAYPOINTS)throw new IllegalArgumentException("Excessive shared waypoints");
        List<WaypointSyncPolicy.Shared> entries=new ArrayList<>(n);
        for(int i=0;i<n;i++)entries.add(new WaypointSyncPolicy.Shared(b.readUUID(),team,b.readUUID(),readPoint(b)));
        return new WaypointSnapshotPayload(seq,team,entries);
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
