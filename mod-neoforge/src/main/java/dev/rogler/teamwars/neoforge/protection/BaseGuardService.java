package dev.rogler.teamwars.neoforge.protection;

import com.mojang.brigadier.Command;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.base.*;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import dev.rogler.teamwars.neoforge.teams.FtbTeamLookup;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import java.util.*;

/** Conservative server-side core-zone protection. This is not a substitute for
 * a full FTB permission audit; Sable/contraptions and other mod block edits need separate tests. */
public final class BaseGuardService {
    private BaseGuardService() {}
    private static BaseCoreSavedData cores(MinecraftServer s) {
        BaseCoreConfig cfg=RoglerTeamWarsMod.baseConfig();
        return BaseCoreSavedData.get(s,cfg.zone(s),cfg.carrySeconds());
    }
    private static UUID owner(MinecraftServer server,ServerLevel level,BlockPos position) {
        if (level!=server.overworld() || !RoglerTeamWarsMod.protectionConfig().baseGuard())return null;
        int radius=RoglerTeamWarsMod.protectionConfig().claimRadiusChunks();
        for(BaseCoreLedger.Entry e:cores(server).ledger().all()) {
            if(e.mode()!=BaseCoreLedger.Mode.PLACED || e.location()==null)continue;
            if(dev.rogler.teamwars.core.base.BaseProtectionPolicy.contains(
                e.location().x(),e.location().z(),position.getX(),position.getZ(),radius))
                return e.teamId();
        }
        return null;
    }
    private static boolean enemy(ServerPlayer player,UUID owner) {
        if(owner==null)return false;
        var a=RoglerTeamWarsMod.assignmentFor(player);
        return !a.validParty() || !owner.equals(a.partyId());
    }
    /** Early cancellation stops most physics mods before the normal break event. */
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock ev) {
        if(!(ev.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level))return;
        if(level.getBlockState(ev.getPos()).is(BaseCoreRegistries.CORE_BLOCK.get())) {
            if(ev.getAction()==PlayerInteractEvent.LeftClickBlock.Action.START)
                dev.rogler.teamwars.neoforge.raid.RaidService.hit(player,ev.getPos());
            ev.setCanceled(true);return;
        }
        if(enemy(player,owner(player.getServer(),level,ev.getPos())))ev.setCanceled(true);
    }
    public static void onBreakSpeed(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed ev) {
        if(!(ev.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level))return;
        if(enemy(player,owner(player.getServer(),level,ev.getPosition().orElse(player.blockPosition()))))
        { ev.setNewSpeed(0f); ev.setCanceled(true); }
    }
    public static void onBreak(BlockEvent.BreakEvent ev) {
        if(!(ev.getPlayer() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level))return;
        if(enemy(player,owner(player.getServer(),level,ev.getPos())))ev.setCanceled(true);
    }
    public static void onPlace(BlockEvent.EntityPlaceEvent ev) {
        if(!(ev.getLevel() instanceof ServerLevel level))return;
        Entity actor=ev.getEntity();
        UUID guard=owner(level.getServer(),level,ev.getPos());
        if(guard==null)return;
        if(!(actor instanceof ServerPlayer player) || enemy(player,guard)) ev.setCanceled(true);
    }
    public static void onInteract(PlayerInteractEvent.RightClickBlock ev) {
        if(!(ev.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level))return;
        if(enemy(player,owner(player.getServer(),level,ev.getPos()))) {
            ev.setCanceled(true);
            ev.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }
    public static void onExplosion(ExplosionEvent.Detonate ev) {
        if(!(ev.getLevel() instanceof ServerLevel level))return;
        // v0.4.0 raid only allows direct valid core hits: never let TNT destroy claimed base blocks.
        ev.getAffectedBlocks().removeIf(p->owner(level.getServer(),level,p)!=null);
    }
    public static void onDamage(LivingIncomingDamageEvent ev) {
        if(!(ev.getEntity() instanceof ServerPlayer victim))return;
        if(!(ev.getSource().getEntity() instanceof ServerPlayer attacker))return;
        if(!(victim.level() instanceof ServerLevel level))return;
        UUID team=owner(victim.getServer(),level,victim.blockPosition());
        if(team==null || !enemy(attacker,team))return;
        var rec=BaseProtectionSavedData.get(victim.getServer()).team(team);
        if(rec!=null && rec.graceUntilMillis>System.currentTimeMillis())ev.setCanceled(true);
    }
    private static CommandSourceStack opSource(MinecraftServer server,BlockPos p) {
        return server.createCommandSourceStack().withPermission(4).withPosition(
            net.minecraft.world.phys.Vec3.atCenterOf(p));
    }
    public static void placed(ServerLevel level,ServerPlayer actor,UUID team,BlockPos at) {
        BaseProtectionSavedData saved=BaseProtectionSavedData.get(level.getServer());
        var r=saved.ensure(team,System.currentTimeMillis(),RoglerTeamWarsMod.protectionConfig().newTeamGraceHours());
        if(RoglerTeamWarsMod.protectionConfig().autoFtbClaims()) {
            var result=FtbClaimBridge.claim(level,team,at.getX()>>4,at.getZ()>>4,
                RoglerTeamWarsMod.protectionConfig().claimRadiusChunks(),r,
                opSource(level.getServer(),at),saved);
            if(result.failed()>0 || !result.detail().equals("OK"))
                actor.sendSystemMessage(Component.literal("[TeamWars] FTB claim sync incomplete ("+result.detail()+")! Native core guard is active. Inform an admin."));
        }
        actor.sendSystemMessage(Component.literal("[TeamWars] Core protection active. Grace until "
            +new java.util.Date(r.graceUntilMillis)+"; raids are OP-controlled, normal block protection stays."));
    }
    public static void packed(ServerLevel level,ServerPlayer actor,UUID team) {
        var saved=BaseProtectionSavedData.get(level.getServer());
        var r=saved.team(team);
        if(r!=null)FtbClaimBridge.release(level,team,r,opSource(level.getServer(),actor.blockPosition()),saved);
        RoglerTeamWarsLogger.info("CORE_PROTECTION_PACK team="+team+" FTB claims released (best effort)");
    }
    private static final Set<UUID> warnedOrphans = new HashSet<>();
    /** Rebuild missing claim ownership for worlds created under older v0.2.x versions. */
    public static void reconcile(MinecraftServer server) {
        var cfg=RoglerTeamWarsMod.protectionConfig();
        BaseProtectionSavedData saved=BaseProtectionSavedData.get(server);
        for(var e:cores(server).ledger().all()) {
            if(e.mode()!=BaseCoreLedger.Mode.PLACED || e.location()==null)continue;
            var r=saved.ensure(e.teamId(),System.currentTimeMillis(),cfg.newTeamGraceHours());
            var state=FtbTeamLookup.state(e.teamId());
            if(state==FtbTeamLookup.State.MISSING && warnedOrphans.add(e.teamId()))
                RoglerTeamWarsLogger.warn("ORPHAN_TEAM: registered core exists but FTB party UUID disappeared "
                    +e.teamId()+". Do not assume same-name party owns it. /teamwars admin orphanlist");
            if(cfg.autoFtbClaims() && state==FtbTeamLookup.State.EXISTS) {
                BlockPos at=new BlockPos(e.location().x(),e.location().y(),e.location().z());
                FtbClaimBridge.claim(server.overworld(),e.teamId(),at.getX()>>4,at.getZ()>>4,
                    cfg.claimRadiusChunks(),r,opSource(server,at),saved);
            }
        }
    }
    public static int ownStatus(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p=src.getPlayerOrException();var a=RoglerTeamWarsMod.assignmentFor(p);
        if(!a.validParty())return 0;
        var r=BaseProtectionSavedData.get(src.getServer()).team(a.partyId());
        long left=r==null?0:Math.max(0,r.graceUntilMillis-System.currentTimeMillis());
        src.sendSuccess(()->Component.literal("[TeamWars] Core Guard: "
            +RoglerTeamWarsMod.protectionConfig().baseGuard()+" | grace left: "+(left/60000)+"m"
            +" | recorded FTB auto claims: "+(r==null?0:r.createdClaims.size())+" | raids enabled: "
            +RoglerTeamWarsMod.protectionConfig().raidsEnabled()),false);
        return Command.SINGLE_SUCCESS;
    }
    public static int setGrace(CommandSourceStack src,ServerPlayer p,int minutes) {
        var a=RoglerTeamWarsMod.assignmentFor(p);if(!a.validParty())return 0;
        return setGraceTeam(src,a.partyId(),minutes);
    }
    public static int setGraceTeam(CommandSourceStack src,UUID team,int minutes) {
        if(cores(src.getServer()).ledger().get(team)==null) {
            src.sendFailure(Component.literal("[TeamWars] No registered core for this team UUID."));return 0;
        }
        long until=System.currentTimeMillis()+minutes*60000L;
        BaseProtectionSavedData.get(src.getServer()).setGrace(team,until);
        src.sendSuccess(()->Component.literal("[TeamWars] Grace time set to "+minutes+"m for team "+team),true);
        return Command.SINGLE_SUCCESS;
    }
    /** Admin-only list of stable team IDs and their current FTB party state; no guesses from names. */
    public static int teams(CommandSourceStack src) {
        var all=cores(src.getServer()).ledger().all();
        src.sendSuccess(()->Component.literal("[TeamWars] Registered team identities: "+all.size()),false);
        for(var e:all) {
            var state=FtbTeamLookup.state(e.teamId());
            var record=BaseProtectionSavedData.get(src.getServer()).team(e.teamId());
            int armor=record==null?0:record.armorLevel;
            src.sendSuccess(()->Component.literal("[TeamWars] "+e.teamId()+" | FTB="+state
                +" | core="+e.mode()+" | armor="+armor+" | auto-claims="+(record==null?0:record.createdClaims.size())),false);
        }
        return Command.SINGLE_SUCCESS;
    }
    /** Admin-only privacy state lookup; intentionally excludes base coordinates. */
    public static int auditClaims(CommandSourceStack src,UUID team) {
        if(FtbTeamLookup.state(team)!=FtbTeamLookup.State.EXISTS) {
            src.sendFailure(Component.literal("[TeamWars] FTB party not found. No privacy claim audit."));return 0;
        }
        String result=FtbClaimBridge.audit(team);
        src.sendSuccess(()->Component.literal("[TeamWars] "+team+": "+result),false);
        return Command.SINGLE_SUCCESS;
    }
    /** OP-only diagnostics: never disclose another team's base to ordinary players. */
    public static int orphanList(CommandSourceStack src) {
        int count=0;
        for(var entry : cores(src.getServer()).ledger().all()) {
            var state=FtbTeamLookup.state(entry.teamId());
            if(state==FtbTeamLookup.State.EXISTS)continue;
            count++;
            src.sendSuccess(()->Component.literal("[TeamWars] " + state + " team="+entry.teamId()
                + " mode="+entry.mode()+" location="+entry.location()
                + " | FTB claims recorded="+claimsCount(src.getServer(),entry.teamId())),false);
        }
        int total=count;
        src.sendSuccess(()->Component.literal("[TeamWars] Non-confirmed teams="+total
            +" (MISSING=orphan, UNAVAILABLE=FTB API failure). Only MISSING may be adopted."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int claimsCount(MinecraftServer server,UUID team) {
        var rec=BaseProtectionSavedData.get(server).team(team);
        return rec==null?0:rec.createdClaims.size();
    }
    /** Explicit admin rescue ONLY when old FTB UUID is confirmed missing. */
    public static int adoptOrphan(CommandSourceStack src,UUID oldTeam,ServerPlayer destinationPlayer) {
        MinecraftServer server=src.getServer();
        if(FtbTeamLookup.state(oldTeam)!=FtbTeamLookup.State.MISSING) {
            src.sendFailure(Component.literal("[TeamWars] Refused: old FTB party is not confirmed deleted."));
            return 0;
        }
        var assignment=RoglerTeamWarsMod.assignmentFor(destinationPlayer);
        if(!assignment.validParty()) {
            src.sendFailure(Component.literal("[TeamWars] Target player needs a valid NEW FTB party."));
            return 0;
        }
        UUID newTeam=assignment.partyId();
        BaseCoreSavedData saved=cores(server);
        var old=saved.ledger().get(oldTeam);
        if(old==null || saved.ledger().get(newTeam)!=null) {
            src.sendFailure(Component.literal("[TeamWars] Source core missing or destination already has a core."));
            return 0;
        }
        var protection=BaseProtectionSavedData.get(server);
        if(claimsCount(server,oldTeam)>0 || claimsCount(server,newTeam)>0) {
            src.sendFailure(Component.literal("[TeamWars] FTB auto-claim bookkeeping exists; inspect and clear claims with FTB admin tools before adoption. No changes made."));
            return 0;
        }
        if(old.mode()==BaseCoreLedger.Mode.PLACED) {
            var p=old.location();
            if(p==null || !"minecraft:overworld".equals(p.dimension()))return 0;
            BlockPos pos=new BlockPos(p.x(),p.y(),p.z());
            ServerLevel world=server.overworld();
            if(!world.hasChunkAt(pos)) {
                src.sendFailure(Component.literal("[TeamWars] Old core chunk not loaded; visit it first. No changes made."));
                return 0;
            }
            if(!world.getBlockState(pos).is(BaseCoreRegistries.CORE_BLOCK.get())) {
                // No physical core: move to ESCROW first, never invent a phantom core.
                saved.ledger().rescueMissingPlaced(oldTeam,old.token(),old.location());
                saved.changed();
                src.sendSuccess(()->Component.literal("[TeamWars] Missing block identified; recovering orphan as ESCROW."),false);
            }
        }
        try {
            // Transfer protection data first; pure ledger transfer cannot fail after preflight.
            protection.transferOrphanRecord(oldTeam,newTeam);
            var updated=saved.ledger().transferOrphan(oldTeam,newTeam,UUID.randomUUID());
            saved.changed();
            RoglerTeamWarsLogger.info("ORPHAN_ADOPT old="+oldTeam+" new="+newTeam+" mode="+updated.mode());
            src.sendSuccess(()->Component.literal("[TeamWars] Adopted core for team="+newTeam
                +" state="+updated.mode()+". Old bearer tokens REVOKED. ESCROW: /teamwars base recover."),true);
            return Command.SINGLE_SUCCESS;
        } catch (IllegalStateException ex) {
            src.sendFailure(Component.literal("[TeamWars] Adoption refused: "+ex.getMessage()));
            return 0;
        }
    }
    public static int claimSync(CommandSourceStack src,ServerPlayer p) {
        if (!RoglerTeamWarsMod.protectionConfig().autoFtbClaims()) {
            src.sendFailure(Component.literal("[TeamWars] FTB auto-claims are OFF for privacy."
                + " Check FTB claim visibility=private for every team, set auto_ftb_claims=true and reload first."));
            return 0;
        }
        var a=RoglerTeamWarsMod.assignmentFor(p);if(!a.validParty())return 0;
        var entry=cores(src.getServer()).ledger().get(a.partyId());
        if(entry==null||entry.mode()!=BaseCoreLedger.Mode.PLACED||entry.location()==null)return 0;
        var saved=BaseProtectionSavedData.get(src.getServer());
        var rec=saved.ensure(a.partyId(),System.currentTimeMillis(),RoglerTeamWarsMod.protectionConfig().newTeamGraceHours());
        BlockPos at=new BlockPos(entry.location().x(),entry.location().y(),entry.location().z());
        var result=FtbClaimBridge.claim(src.getServer().overworld(),a.partyId(),at.getX()>>4,at.getZ()>>4,
            RoglerTeamWarsMod.protectionConfig().claimRadiusChunks(),rec,opSource(src.getServer(),at),saved);
        src.sendSuccess(()->Component.literal("[TeamWars] FTB claims created="+result.created()+" existing="
            +result.existing()+" rejected="+result.failed()+" detail="+result.detail()),false);
        return Command.SINGLE_SUCCESS;
    }
}
