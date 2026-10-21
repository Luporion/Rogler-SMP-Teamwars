package dev.rogler.teamwars.neoforge.map;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import dev.rogler.teamwars.neoforge.teams.FtbPartyReader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server authority for explicitly shared team waypoints. Never sees personal waypoints. */
public final class TeamWaypointService {
    private static final FtbPartyReader FTB=new FtbPartyReader();
    private static final Map<UUID,Long> LAST_MUTATION=new HashMap<>();
    private static long sequence;
    private static final Map<UUID,Integer> PREVIOUS_HASH=new HashMap<>();
    private static final Map<UUID,Long> LAST_SENT=new HashMap<>();
    private TeamWaypointService(){}
    public static void clear(){LAST_MUTATION.clear();PREVIOUS_HASH.clear();LAST_SENT.clear();sequence=0;}
    public static void logout(UUID id){LAST_MUTATION.remove(id);PREVIOUS_HASH.remove(id);LAST_SENT.remove(id);}
    public static void handle(WaypointMutationPayload req, IPayloadContext context){
        context.enqueueWork(()->{
            if(!(context.player() instanceof ServerPlayer player))return;
            var server=player.getServer();if(server==null)return;
            long tick=server.getTickCount();UUID self=player.getUUID();
            long prior=LAST_MUTATION.getOrDefault(self,Long.MIN_VALUE);
            if(prior!=Long.MIN_VALUE && tick-prior<10)return;
            LAST_MUTATION.put(self,tick);
            TeamAssignmentPolicy.Party party=FTB.partyFor(self);
            if(party==null||!party.members().contains(self))return;
            var storage=TeamWaypointSavedData.get(server);
            boolean ok=req.revoke()?storage.revoke(req.id(),self,party.id()):storage.add(party.id(),self,req.point());
            player.sendSystemMessage(Component.literal("[Rogler] Team-Wegpunkt "+(ok?(req.revoke()?"zurueckgezogen":"freigegeben"):"abgelehnt (Limit oder Berechtigung)")));
        });
    }
    public static void tick(MinecraftServer server){
        if(server.getTickCount()%20!=0)return;
        var storage=TeamWaypointSavedData.get(server);
        Map<UUID,TeamAssignmentPolicy.Party> cache=new HashMap<>();
        // Each destination is evaluated independently. OP and public FTB modes do not bypass membership.
        for(ServerPlayer viewer:server.getPlayerList().getPlayers()){
            UUID id=viewer.getUUID();var party=cache.computeIfAbsent(id,FTB::partyFor);
            if(party==null||!party.members().contains(id)){
                send(viewer,WaypointSnapshotPayload.NO_TEAM,java.util.List.of(),server.getTickCount());
                continue;
            }
            var valid=storage.all().stream().filter(e->e.team().equals(party.id()))
                .filter(e->{var ownerParty=cache.computeIfAbsent(e.owner(),FTB::partyFor);
                    return ownerParty!=null&&ownerParty.id().equals(party.id())&&ownerParty.members().contains(e.owner());})
                .toList();
            var allowed=WaypointSyncPolicy.visible(valid,id,party.id(),party.members());
            send(viewer,party.id(),allowed,server.getTickCount());
        }
    }
    private static void send(ServerPlayer viewer,UUID team,java.util.List<WaypointSyncPolicy.Shared> allowed,long tick){
        UUID id=viewer.getUUID();int fingerprint=java.util.Objects.hash(team,allowed);
        if(PREVIOUS_HASH.getOrDefault(id,Integer.MIN_VALUE)==fingerprint && tick-LAST_SENT.getOrDefault(id,0L)<80)return;
        PREVIOUS_HASH.put(id,fingerprint);LAST_SENT.put(id,tick);
        PacketDistributor.sendToPlayer(viewer,new WaypointSnapshotPayload(++sequence,team,allowed));
    }
}
