package dev.rogler.teamwars.neoforge.ui;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** No arbitrary command or server-side path can be submitted by the GUI. */
public record ControlCenterRequestPayload(String operation, String key, String value) implements CustomPacketPayload {
    public static final Type<ControlCenterRequestPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"ui_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ControlCenterRequestPayload> STREAM_CODEC =
        StreamCodec.of(ControlCenterRequestPayload::write,ControlCenterRequestPayload::read);
    public ControlCenterRequestPayload {
        if(operation==null || operation.length()>16 || key==null || key.length()>64 || value==null || value.length()>64)
            throw new IllegalArgumentException("Invalid UI request");
    }
    private static void write(RegistryFriendlyByteBuf b,ControlCenterRequestPayload p){
        b.writeUtf(p.operation,16);b.writeUtf(p.key,64);b.writeUtf(p.value,64);
    }
    private static ControlCenterRequestPayload read(RegistryFriendlyByteBuf b){
        return new ControlCenterRequestPayload(b.readUtf(16),b.readUtf(64),b.readUtf(64));
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
