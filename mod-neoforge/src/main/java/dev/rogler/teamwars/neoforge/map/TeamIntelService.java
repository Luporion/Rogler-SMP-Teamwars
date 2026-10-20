package dev.rogler.teamwars.neoforge.map;

import dev.rogler.teamwars.core.TeamIntelPolicy;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.base.BaseCoreSavedData;
import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.teams.FtbPartyReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends per-viewer packets only. Team data is freshly checked against FTB's
 * effective-party roster for BOTH viewer and each online candidate member.
 * No broadcast and no disk storage. Coordinates change with a 2s cadence.
 */
public final class TeamIntelService {
    private static final FtbPartyReader FTB = new FtbPartyReader();
    private static final Map<UUID,Long> SEQUENCES = new HashMap<>();
    private TeamIntelService() {}

    public static void reset() { SEQUENCES.clear(); }
    public static void logout(UUID viewer) { SEQUENCES.remove(viewer); }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 0) return;
        var players=server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        Map<UUID, TeamAssignmentPolicy.Party> verified=new HashMap<>();
        List<TeamIntelPolicy.Presence> online=new ArrayList<>(players.size());
        for(ServerPlayer member:players){
            UUID id=member.getUUID();
            verified.put(id,FTB.partyFor(id));
            online.add(new TeamIntelPolicy.Presence(id,member.getGameProfile().getName(),
                member.level().dimension().location().toString(),member.blockPosition().getX(),member.blockPosition().getY(),member.blockPosition().getZ()));
        }
        var baseConfig=RoglerTeamWarsMod.baseConfig();
        var spawn=server.overworld().getSharedSpawnPos();
        for(ServerPlayer viewer:players){
            UUID id=viewer.getUUID();
            var authorized=TeamIntelPolicy.authorized(id,verified.get(id),verified,online);
            long sequence=SEQUENCES.merge(id,1L,Long::sum);
            PacketDistributor.sendToPlayer(viewer,new TeamIntelPayload(sequence,
                viewer.level().dimension().location().toString(),spawn.getX(),spawn.getZ(),
                baseConfig.innerRadius(),baseConfig.outerRadius(),authorized));
            // Personal bed/anchor spawn remains private. Base coordinates are only
            // sent when the current effective FTB party authorizes this viewer.
            String respawnDim="",coreDim="";
            int rx=0,ry=0,rz=0,bx=0,by=0,bz=0;
            var respawn=viewer.getRespawnPosition();
            if(respawn!=null){
                respawnDim=viewer.getRespawnDimension().location().toString();
                rx=respawn.getX();ry=respawn.getY();rz=respawn.getZ();
            }
            var party=verified.get(id);
            if(party!=null && party.members().contains(id)){
                var saved=BaseCoreSavedData.get(server,baseConfig.zone(server),baseConfig.carrySeconds());
                var core=saved.ledger().get(party.id());
                if(core!=null && core.mode()==BaseCoreLedger.Mode.PLACED && core.location()!=null){
                    var position=core.location();
                    coreDim=position.dimension();bx=position.x();by=position.y();bz=position.z();
                }
            }
            PacketDistributor.sendToPlayer(viewer,new TeamNavigationPayload(sequence,
                respawnDim,rx,ry,rz,coreDim,bx,by,bz));
        }
        SEQUENCES.keySet().retainAll(players.stream().map(ServerPlayer::getUUID).toList());
    }
}
