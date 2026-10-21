package dev.rogler.teamwars.neoforge.map;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Explicit opt-in client request. The server ignores any client-supplied team UUID. */
public record WaypointMutationPayload(boolean revoke, UUID id, WaypointSyncPolicy.Point point) implements CustomPacketPayload {
    public static final Type<WaypointMutationPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"waypoint_mutation"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WaypointMutationPayload> STREAM_CODEC=StreamCodec.of(WaypointMutationPayload::write,WaypointMutationPayload::read);
    public WaypointMutationPayload{
        if(id==null || (!revoke && point==null))throw new IllegalArgumentException("Invalid waypoint mutation");
    }
    private static void write(RegistryFriendlyByteBuf b,WaypointMutationPayload p){
        b.writeBoolean(p.revoke());b.writeUUID(p.id());
        if(!p.revoke())WaypointSnapshotPayload.writePoint(b,p.point());
    }
    private static WaypointMutationPayload read(RegistryFriendlyByteBuf b){
        boolean revoke=b.readBoolean();UUID id=b.readUUID();
        return new WaypointMutationPayload(revoke,id,revoke?null:WaypointSnapshotPayload.readPoint(b));
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
