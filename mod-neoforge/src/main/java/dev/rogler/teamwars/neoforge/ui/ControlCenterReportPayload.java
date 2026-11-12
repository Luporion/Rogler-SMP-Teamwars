package dev.rogler.teamwars.neoforge.ui;

import dev.rogler.teamwars.core.ui.DiagnosticPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A bounded, server-authored diagnostic page; scope and authority are checked on every request. */
public record ControlCenterReportPayload(String kind,UUID target,String title,int offset,int total,
                                        long tick,List<DiagnosticPolicy.Row> rows) implements CustomPacketPayload {
    public static final Type<ControlCenterReportPayload> TYPE=new Type<>(
        ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"ui_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ControlCenterReportPayload> STREAM_CODEC=
        StreamCodec.of(ControlCenterReportPayload::write,ControlCenterReportPayload::read);
    public ControlCenterReportPayload {
        if(!DiagnosticPolicy.isReport(kind) || title==null || title.length()>64
            || offset<0 || offset>=DiagnosticPolicy.MAX_ROWS || total<0 || total>DiagnosticPolicy.MAX_ROWS
            || tick<0 || rows==null || rows.size()>DiagnosticPolicy.MAX_PAGE
            || rows.size()>Math.max(0,total-offset))throw new IllegalArgumentException("Invalid report");
        rows=List.copyOf(rows);
    }
    private static void write(RegistryFriendlyByteBuf b,ControlCenterReportPayload p) {
        b.writeUtf(p.kind,8);b.writeBoolean(p.target!=null);if(p.target!=null)b.writeUUID(p.target);
        b.writeUtf(p.title,64);b.writeVarInt(p.offset);b.writeVarInt(p.total);b.writeLong(p.tick);
        b.writeVarInt(p.rows.size());
        for(var row:p.rows){b.writeUtf(row.label(),48);b.writeUtf(row.value(),180);}
    }
    private static ControlCenterReportPayload read(RegistryFriendlyByteBuf b) {
        String kind=b.readUtf(8);UUID target=b.readBoolean()?b.readUUID():null;
        String title=b.readUtf(64);int offset=b.readVarInt(),total=b.readVarInt();long tick=b.readLong();
        int size=b.readVarInt();if(size<0 || size>DiagnosticPolicy.MAX_PAGE)throw new IllegalArgumentException("Too many diagnostic rows");
        var rows=new ArrayList<DiagnosticPolicy.Row>();
        for(int i=0;i<size;i++)rows.add(new DiagnosticPolicy.Row(b.readUtf(48),b.readUtf(180)));
        return new ControlCenterReportPayload(kind,target,title,offset,total,tick,rows);
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
