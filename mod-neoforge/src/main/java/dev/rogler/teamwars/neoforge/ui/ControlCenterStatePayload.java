package dev.rogler.teamwars.neoforge.ui;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.LinkedHashMap;
import java.util.Map;

/** View-specific response. Server sends settings only to authorized operators. */
public record ControlCenterStatePayload(int permission, String team, String message,
                                        int online, Map<String,String> settings) implements CustomPacketPayload {
    public static final Type<ControlCenterStatePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"ui_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ControlCenterStatePayload> STREAM_CODEC =
        StreamCodec.of(ControlCenterStatePayload::write,ControlCenterStatePayload::read);
    public ControlCenterStatePayload {
        if(permission<0 || permission>4 || team==null || team.length()>128 || message==null || message.length()>220
            || online<0 || online>100000 || settings==null || settings.size()>32)
            throw new IllegalArgumentException("Invalid control center state");
        settings=Map.copyOf(settings);
    }
    private static void write(RegistryFriendlyByteBuf b, ControlCenterStatePayload p){
        b.writeVarInt(p.permission);b.writeUtf(p.team,128);b.writeUtf(p.message,220);b.writeVarInt(p.online);
        b.writeVarInt(p.settings.size());
        p.settings.forEach((key,value)->{b.writeUtf(key,64);b.writeUtf(value,32);});
    }
    private static ControlCenterStatePayload read(RegistryFriendlyByteBuf b){
        int permission=b.readVarInt();String team=b.readUtf(128), message=b.readUtf(220);int online=b.readVarInt();
        int size=b.readVarInt();if(size<0 || size>32)throw new IllegalArgumentException("Too many settings");
        Map<String,String> map=new LinkedHashMap<>();
        for(int i=0;i<size;i++){
            String key=b.readUtf(64), value=b.readUtf(32);
            if(map.put(key,value)!=null)throw new IllegalArgumentException("Duplicate setting");
        }
        return new ControlCenterStatePayload(permission,team,message,online,map);
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
