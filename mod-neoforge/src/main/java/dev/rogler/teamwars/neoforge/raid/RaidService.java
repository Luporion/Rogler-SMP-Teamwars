package dev.rogler.teamwars.neoforge.raid;

import com.mojang.brigadier.Command;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.core.base.RaidPolicy;
import dev.rogler.teamwars.core.base.CoreUpgradePolicy;
import dev.rogler.teamwars.neoforge.teams.FtbTeamLookup;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.base.*;
import dev.rogler.teamwars.neoforge.protection.*;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** v0.4.0 narrow raid MVP: explicit OP window, direct validated core hits only.
 * No bypass of FTB block permissions and no general destruction during raids. */
public final class RaidService {
    private RaidService() {}
    private static BaseCoreSavedData cores(MinecraftServer server){
        var cfg=RoglerTeamWarsMod.baseConfig();
        return BaseCoreSavedData.get(server,cfg.zone(server),cfg.carrySeconds());
    }
    private static void msg(CommandSourceStack src,String message){
        src.sendSuccess(()->Component.literal("[TeamWars] "+message),false);
    }
    private static boolean onlineDefender(MinecraftServer server,UUID defender){
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            var a=RoglerTeamWarsMod.assignmentFor(p);
            if(a.validParty() && defender.equals(a.partyId()))return true;
        }
        return false;
    }
    public static boolean active(MinecraftServer server,UUID defender){
        var raid=RaidSavedData.get(server).raid(defender);
        return raid!=null && raid.hp()>0 && raid.untilMillis()>System.currentTimeMillis();
    }
    public static void tick(MinecraftServer server){
        var data=RaidSavedData.get(server);
        long now=System.currentTimeMillis();
        for(var raid:data.raids()){
            if(raid.untilMillis()<=now || !onlineDefender(server,raid.defender())){
                data.close(raid.defender());
                RoglerTeamWarsLogger.info("RAID_CLOSED defender="+raid.defender()+" reason=expired_or_offline");
            }
        }
    }
    public static int open(CommandSourceStack src,ServerPlayer defender,int minutes){
        MinecraftServer server=src.getServer();
        if(!RoglerTeamWarsMod.protectionConfig().raidsEnabled()){
            src.sendFailure(Component.literal("[TeamWars] Raids are disabled in roglerteamwars-protection.properties."));return 0;
        }
        var assignment=RoglerTeamWarsMod.assignmentFor(defender);
        if(!assignment.validParty()){src.sendFailure(Component.literal("[TeamWars] Defender needs an FTB party."));return 0;}
        return openTeam(src,assignment.partyId(),minutes);
    }
    public static int openTeam(CommandSourceStack src,UUID team,int minutes){
        MinecraftServer server=src.getServer();
        if(!RoglerTeamWarsMod.protectionConfig().raidsEnabled()) {
            src.sendFailure(Component.literal("[TeamWars] Raids are disabled.")); return 0;
        }
        if(FtbTeamLookup.state(team)!=FtbTeamLookup.State.EXISTS || !onlineDefender(server,team)) {
            src.sendFailure(Component.literal("[TeamWars] Team missing or no defender online.")); return 0;
        }
        var entry=cores(server).ledger().get(team);
        if(entry==null || entry.mode()!=BaseCoreLedger.Mode.PLACED || entry.location()==null){
            src.sendFailure(Component.literal("[TeamWars] Defender has no placed core."));return 0;
        }
        long now=System.currentTimeMillis();
        var rec=BaseProtectionSavedData.get(server).team(team);
        if(rec!=null && rec.graceUntilMillis>now){
            src.sendFailure(Component.literal("[TeamWars] Defender is still protected by grace."));return 0;
        }
        var data=RaidSavedData.get(server);
        if(data.raid(team)!=null){src.sendFailure(Component.literal("[TeamWars] Raid already open. End it first."));return 0;}
        int armor=rec==null?0:rec.armorLevel;
        int hp=CoreUpgradePolicy.maxHealth(armor);
        data.open(new RaidPolicy.Window(team,null,now+minutes*60000L,hp,0L));
        RoglerTeamWarsLogger.info("RAID_OPEN defender="+team+" duration_minutes="+minutes);
        for(ServerPlayer p:server.getPlayerList().getPlayers())p.sendSystemMessage(
            Component.literal("[TeamWars] An OP has opened a core-only raid window for a team. Regular blocks remain protected."));
        msg(src,"Raid started for "+team+"; " + minutes+"m; core health="+hp);
        return Command.SINGLE_SUCCESS;
    }
    public static int stop(CommandSourceStack src,ServerPlayer defender){
        var a=RoglerTeamWarsMod.assignmentFor(defender);
        if(!a.validParty())return 0;
        return stopTeam(src,a.partyId());
    }
    public static int stopTeam(CommandSourceStack src,UUID team){
        RaidSavedData.get(src.getServer()).close(team);
        msg(src,"Raid closed for "+team);return Command.SINGLE_SUCCESS;
    }
    /** Called for a single server-side START attack against the actual core block. */
    public static void hit(ServerPlayer attacker,BlockPos target){
        if(!RoglerTeamWarsMod.protectionConfig().raidsEnabled() || attacker.isCreative() || attacker.isSpectator())return;
        if(!(attacker.level() instanceof ServerLevel level) || level!=attacker.getServer().overworld())return;
        if(attacker.distanceToSqr(Vec3.atCenterOf(target))>36.0)return;
        var own=RoglerTeamWarsMod.assignmentFor(attacker);
        if(!own.validParty())return;
        MinecraftServer server=attacker.getServer();
        var saved=cores(server);
        BaseCoreLedger.Entry defended=null;
        var position=BaseCoreItem.position(level,target);
        for(var e:saved.ledger().all()){
            if(e.mode()==BaseCoreLedger.Mode.PLACED && position.equals(e.location())){defended=e;break;}
        }
        if(defended==null || own.partyId().equals(defended.teamId()))return;
        UUID team=defended.teamId();
        var raids=RaidSavedData.get(server);
        var raid=raids.raid(team);
        long now=System.currentTimeMillis();
        var rec=BaseProtectionSavedData.get(server).team(team);
        long grace=rec==null?Long.MAX_VALUE:rec.graceUntilMillis;
        if(!RaidPolicy.eligible(raid,team,own.partyId(),now,grace,onlineDefender(server,team)))return;
        RaidPolicy.Window next;
        try{next=RaidPolicy.hit(raid,own.partyId(),now);}catch(IllegalStateException ignored){return;}
        if(next.hp()>0){
            raids.update(next);
            attacker.sendSystemMessage(Component.literal("[TeamWars] Core hit! Remaining health: "+next.hp()+" HP"));
            return;
        }
        if(!level.isLoaded(target) || !level.getBlockState(target).is(BaseCoreRegistries.CORE_BLOCK.get()))return;
        // Remove the world block first, then transition the ONE registered bearer token.
        if(!level.removeBlock(target,false))return;
        try{
            saved.ledger().defeatPlaced(team,defended.token(),defended.location());
        }catch(IllegalStateException ex){
            RoglerTeamWarsLogger.warn("RAID_CORE_REMOVE_LEDGER_MISMATCH team="+team+" "+ex.getMessage());
            // Keep the losing party recoverable, never award a point on an inconsistent transition.
            try{saved.ledger().rescueMissingPlaced(team,defended.token(),defended.location());saved.changed();}
            catch(IllegalStateException ignored){}
            raids.close(team);return;
        }
        saved.changed();
        // Unclaim ONLY TeamWars-created chunks. Never touch manually created claims.
        BaseGuardService.packed(level,attacker,team);
        var protection=BaseProtectionSavedData.get(server);
        protection.setGrace(team,now+RoglerTeamWarsMod.protectionConfig().newTeamGraceHours()*3600000L);
        raids.close(team);
        raids.score(own.partyId());
        RoglerTeamWarsLogger.info("RAID_WIN defender="+team+" attacker="+own.partyId()+" core="+target+" points=1");
        for(ServerPlayer p:server.getPlayerList().getPlayers())p.sendSystemMessage(
            Component.literal("[TeamWars] Core raid victory: team "+own.displayName()+" gains 1 point. Defenders may /teamwars base recover."));
    }
    public static int scores(CommandSourceStack src){
        var score=RaidSavedData.get(src.getServer()).scores();
        msg(src,"War scores: "+(score.isEmpty()?"no points yet":score.size()+" scoring teams"));
        score.entrySet().stream().sorted(Map.Entry.<UUID,Integer>comparingByValue().reversed())
            .forEach(e->msg(src,e.getKey()+" : "+e.getValue()+" points"));
        return Command.SINGLE_SUCCESS;
    }
    public static int status(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p=src.getPlayerOrException();
        var a=RoglerTeamWarsMod.assignmentFor(p);
        if(!a.validParty())return 0;
        var r=RaidSavedData.get(src.getServer()).raid(a.partyId());
        msg(src,r==null?"No raid against your base":
            "Your base raid: "+Math.max(0L,(r.untilMillis()-System.currentTimeMillis())/1000)+"s left, HP="+r.hp());
        return Command.SINGLE_SUCCESS;
    }
}
