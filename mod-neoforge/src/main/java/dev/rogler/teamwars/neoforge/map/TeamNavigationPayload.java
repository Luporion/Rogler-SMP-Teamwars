package dev.rogler.teamwars.neoforge.map;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Private per-player navigation targets. Base data is present only for the viewer's current FTB team. */
public record TeamNavigationPayload(long sequence, String respawnDim, int respawnX, int respawnY, int respawnZ,
                                    String coreDim, int coreX, int coreY, int coreZ) implements CustomPacketPayload {
    public static final Type<TeamNavigationPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"team_navigation"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TeamNavigationPayload> STREAM_CODEC =
        StreamCodec.of(TeamNavigationPayload::write,TeamNavigationPayload::read);
    public TeamNavigationPayload{
        if(respawnDim==null||coreDim==null||respawnDim.length()>128||coreDim.length()>128)
            throw new IllegalArgumentException("Invalid navigation dimension");
    }
    public static void write(RegistryFriendlyByteBuf b,TeamNavigationPayload p){
        b.writeLong(p.sequence);b.writeUtf(p.respawnDim,128);
        b.writeInt(p.respawnX);b.writeInt(p.respawnY);b.writeInt(p.respawnZ);
        b.writeUtf(p.coreDim,128);b.writeInt(p.coreX);b.writeInt(p.coreY);b.writeInt(p.coreZ);
    }
    public static TeamNavigationPayload read(RegistryFriendlyByteBuf b){
        return new TeamNavigationPayload(b.readLong(),b.readUtf(128),b.readInt(),b.readInt(),b.readInt(),
            b.readUtf(128),b.readInt(),b.readInt(),b.readInt());
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
