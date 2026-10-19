package dev.rogler.teamwars.neoforge.map;

import dev.rogler.teamwars.core.TeamIntelClientState;
import dev.rogler.teamwars.core.TeamIntelPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Small, server->viewer-only team intel. Never broadcast or write coordinates to disk. */
public record TeamIntelPayload(long sequence, String dimension, int spawnX, int spawnZ, int innerRadius,
                               int outerRadius, List<TeamIntelPolicy.Presence> teammates) implements CustomPacketPayload {
    public static final Type<TeamIntelPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"team_intel"));

    public TeamIntelPayload {
        teammates = List.copyOf(teammates);
        if (teammates.size() > TeamIntelPolicy.MAX_MARKERS) throw new IllegalArgumentException("Too many teammates");
        if (dimension.length() > 128) throw new IllegalArgumentException("Dimension too long");
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TeamIntelPayload> STREAM_CODEC =
        StreamCodec.of(TeamIntelPayload::write, TeamIntelPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, TeamIntelPayload value) {
        buf.writeLong(value.sequence());
        buf.writeUtf(value.dimension(),128);
        buf.writeInt(value.spawnX()); buf.writeInt(value.spawnZ());
        buf.writeVarInt(value.innerRadius()); buf.writeVarInt(value.outerRadius());
        buf.writeVarInt(value.teammates().size());
        for (TeamIntelPolicy.Presence p : value.teammates()) {
            buf.writeUUID(p.playerId());
            buf.writeUtf(p.name(),32);
            buf.writeUtf(p.dimension(),128);
            buf.writeInt(p.x()); buf.writeInt(p.z());
        }
    }

    private static TeamIntelPayload read(RegistryFriendlyByteBuf buf) {
        long sequence=buf.readLong();
        String dimension=buf.readUtf(128);
        int spawnX=buf.readInt(), spawnZ=buf.readInt();
        int inner=buf.readVarInt(), outer=buf.readVarInt();
        int size=buf.readVarInt();
        if (size < 0 || size > TeamIntelPolicy.MAX_MARKERS)
            throw new IllegalArgumentException("Invalid teammate count: " + size);
        List<TeamIntelPolicy.Presence> mates=new ArrayList<>(size);
        for (int i=0;i<size;i++){
            UUID id=buf.readUUID();
            String name=buf.readUtf(32), dim=buf.readUtf(128);
            int x=buf.readInt(),z=buf.readInt();
            mates.add(new TeamIntelPolicy.Presence(id,name,dim,x,z));
        }
        // Validate before presenting untrusted payload to HUD.
        new TeamIntelClientState.Snapshot(sequence,dimension,spawnX,spawnZ,inner,outer,mates);
        return new TeamIntelPayload(sequence,dimension,spawnX,spawnZ,inner,outer,mates);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
