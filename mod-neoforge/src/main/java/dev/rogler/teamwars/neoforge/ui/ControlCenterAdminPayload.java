package dev.rogler.teamwars.neoforge.ui;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.UUID;

/** Operator-only, bounded server-authored directory/audit view. Never sent to ordinary players. */
public record ControlCenterAdminPayload(String kind, int offset, int total,
        List<TeamRow> teams, List<String> audit) implements CustomPacketPayload {
    public record TeamRow(UUID id, String name, int online, int members, String core,
                          String location, int graceMinutes, int score, boolean raid) {
        public TeamRow {
            if(id==null || name==null || name.length()>48 || online<0 || online>1000
                || members<0 || members>1000 || core==null || core.length()>20
                || location==null || location.length()>75 || graceMinutes<0 || graceMinutes>1000000
                || score<0)throw new IllegalArgumentException("Invalid team summary");
        }
    }
    public static final Type<ControlCenterAdminPayload> TYPE=new Type<>(
        ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"ui_admin_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ControlCenterAdminPayload> STREAM_CODEC =
        StreamCodec.of(ControlCenterAdminPayload::write,ControlCenterAdminPayload::read);
    public ControlCenterAdminPayload {
        if(!kind.equals("teams") && !kind.equals("audit") && !kind.equals("status") || offset<0 || offset>1000 || total<0
            || total>100000 || teams==null || teams.size()>5 || audit==null || audit.size()>5)
            throw new IllegalArgumentException("Invalid admin snapshot");
        if(audit.stream().anyMatch(s->s==null || s.length()>180))
            throw new IllegalArgumentException("Invalid audit row");
        teams=List.copyOf(teams);audit=List.copyOf(audit);
    }
    private static void write(RegistryFriendlyByteBuf b,ControlCenterAdminPayload p){
        b.writeUtf(p.kind,8);b.writeVarInt(p.offset);b.writeVarInt(p.total);
        b.writeVarInt(p.teams.size());
        for(TeamRow r:p.teams){
            b.writeUUID(r.id);b.writeUtf(r.name,48);b.writeVarInt(r.online);b.writeVarInt(r.members);
            b.writeUtf(r.core,20);b.writeUtf(r.location,75);b.writeVarInt(r.graceMinutes);
            b.writeVarInt(r.score);b.writeBoolean(r.raid);
        }
        b.writeVarInt(p.audit.size());for(String a:p.audit)b.writeUtf(a,180);
    }
    private static ControlCenterAdminPayload read(RegistryFriendlyByteBuf b){
        String kind=b.readUtf(8);int offset=b.readVarInt(),total=b.readVarInt();
        int count=b.readVarInt();if(count<0||count>5)throw new IllegalArgumentException("Too many team rows");
        var rows=new java.util.ArrayList<TeamRow>();
        for(int i=0;i<count;i++)rows.add(new TeamRow(b.readUUID(),b.readUtf(48),b.readVarInt(),
            b.readVarInt(),b.readUtf(20),b.readUtf(75),b.readVarInt(),b.readVarInt(),b.readBoolean()));
        count=b.readVarInt();if(count<0||count>5)throw new IllegalArgumentException("Too many audit rows");
        var audit=new java.util.ArrayList<String>();for(int i=0;i<count;i++)audit.add(b.readUtf(180));
        return new ControlCenterAdminPayload(kind,offset,total,rows,audit);
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
