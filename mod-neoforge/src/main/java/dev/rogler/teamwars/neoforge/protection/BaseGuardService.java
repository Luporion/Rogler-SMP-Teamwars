package dev.rogler.teamwars.neoforge.protection;

import com.mojang.brigadier.Command;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.base.*;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import dev.rogler.teamwars.neoforge.teams.FtbTeamLookup;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import dev.rogler.teamwars.core.base.CoreIntegrityPolicy;
import dev.rogler.teamwars.core.base.CoreExplosionIndex;
import dev.rogler.teamwars.core.base.CoreRecoveryPolicy;
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
    // Same logical server thread as normal Minecraft world updates. Never cache a
    // protection index across explosions: placement/relocation must take effect immediately.
    private static CoreRecoveryPolicy recoveryPolicy = new CoreRecoveryPolicy();
    private static long measuredExplosions, measuredPositions, preventedBlocks;
    private static long totalHandlerNanos, maxHandlerNanos, slowHandlers, lastSlowLogTick = Long.MIN_VALUE;
    private static final long SLOW_HANDLER_NS = 25_000_000L;
    private static final long LOG_INTERVAL_TICKS = 1200L;

    /** Discard volatile observations when an integrated/dedicated server is switched. */
    public static void resetRuntimeState() {
        recoveryPolicy = new CoreRecoveryPolicy();
        measuredExplosions = measuredPositions = preventedBlocks = 0L;
        totalHandlerNanos = maxHandlerNanos = slowHandlers = 0L;
        lastSlowLogTick = Long.MIN_VALUE;
        warnedOrphans.clear();
    }
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
        if (!(ev.getLevel() instanceof ServerLevel level) || level != level.getServer().overworld()
            || !RoglerTeamWarsMod.protectionConfig().baseGuard()) return;
        var affected = ev.getAffectedBlocks();
        if (affected.isEmpty()) return;
        long began = System.nanoTime();
        // Build the exact 3x3/7x7 chunk projection ONCE for this event. No world or
        // FTB query inside the affected-block loop, no forced chunk loads, no stale cache.
        int radius = RoglerTeamWarsMod.protectionConfig().claimRadiusChunks();
        List<CoreExplosionIndex.Center> centers = new ArrayList<>();
        for (var entry : cores(level.getServer()).ledger().all()) {
            if (entry.mode() != BaseCoreLedger.Mode.PLACED || entry.location() == null) continue;
            var at = entry.location();
            if (!"minecraft:overworld".equals(at.dimension())) continue;
            centers.add(new CoreExplosionIndex.Center(entry.teamId(), at.x(), at.z()));
        }
        CoreExplosionIndex index = CoreExplosionIndex.build(centers, radius);
        int before = affected.size();
        if (index.coveredChunks() != 0)
            affected.removeIf(p -> index.protectsBlock(p.getX(), p.getZ()));
        long elapsed = System.nanoTime() - began;
        measuredExplosions++;
        measuredPositions += before;
        preventedBlocks += before - affected.size();
        totalHandlerNanos += elapsed;
        maxHandlerNanos = Math.max(maxHandlerNanos, elapsed);
        if (elapsed >= SLOW_HANDLER_NS) {
            slowHandlers++;
            long tick = level.getServer().getTickCount();
            if (lastSlowLogTick == Long.MIN_VALUE || tick - lastSlowLogTick >= LOG_INTERVAL_TICKS) {
                lastSlowLogTick = tick;
                RoglerTeamWarsLogger.warn("EXPLOSION_GUARD_SLOW handler_ms=" + (elapsed / 1_000_000L)
                    + " affected=" + before + " protected=" + (before - affected.size())
                    + " active_cores=" + centers.size() + " protected_chunks=" + index.coveredChunks()
                    + " (excludes Level.explode and third-party callback time)");
            }
        }
    }
    /** Read-only, bounded counters; timings cover ONLY this handler, not the entire explosion. */
    public static int explosionStats(CommandSourceStack src) {
        long n=measuredExplosions, pos=measuredPositions, avoided=preventedBlocks;
        long averageMicros=n==0?0:(totalHandlerNanos/n)/1000L;
        long worstMicros=maxHandlerNanos/1000L;
        src.sendSuccess(()->Component.literal("[Rogler Core] Explosion guard: events="+n
            +" affected_positions="+pos+" protected_positions="+avoided
            +" avg_handler_us="+averageMicros+" max_handler_us="+worstMicros
            +" over_25ms="+slowHandlers+" | excludes Warium explosion / loot / chunk generation"),false);
        return Command.SINGLE_SUCCESS;
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
    /**
     * Compensating transaction for direct third-party world mutations (e.g. some modded
     * nuclear weapons bypass ExplosionEvent and regular BreakEvent). Server tick ONLY;
     * checks loaded chunks without force-loading and never overwrites a non-air block.
     * If reconstruction is not possible, quarantine the bearer in ESCROW and release
     * only our recorded FTB claims (with best-effort retries). No victory/score is awarded.
     */
    public static void verifyPhysicalCores(MinecraftServer server) {
        BaseCoreSavedData db=cores(server);
        BaseProtectionSavedData protections=BaseProtectionSavedData.get(server);
        ServerLevel level=server.overworld();
        for(var entry:java.util.List.copyOf(db.ledger().all())) {
            UUID team=entry.teamId();
            var rec=protections.team(team);
            boolean placed=entry.mode()==BaseCoreLedger.Mode.PLACED && entry.location()!=null;
            BlockPos pos=placed?new BlockPos(entry.location().x(),entry.location().y(),entry.location().z()):null;
            boolean loaded=pos!=null && level.hasChunkAt(pos);
            boolean physical=loaded && level.getBlockState(pos).is(BaseCoreRegistries.CORE_BLOCK.get());
            boolean air=loaded && level.getBlockState(pos).isAir();
            var action=CoreIntegrityPolicy.decide(placed,loaded,physical,air,
                rec!=null && !rec.createdClaims.isEmpty());
            if (!placed) recoveryPolicy.resolved(team);
            else if (!loaded) recoveryPolicy.unloaded(team);
            else if (physical) recoveryPolicy.healthy(team, System.currentTimeMillis());
            if (action == CoreIntegrityPolicy.Action.RESTORE_AIR) {
                CoreRecoveryPolicy.Decision decision = recoveryPolicy.missingAir(team, System.currentTimeMillis());
                if (decision == CoreRecoveryPolicy.Decision.WAIT) continue;
                if (decision == CoreRecoveryPolicy.Decision.QUARANTINE) {
                    RoglerTeamWarsLogger.warn("CORE_REPEATED_EXTERNAL_DESTRUCTION team="+team
                        +"; bounded retries exhausted, quarantine instead of infinite rebuilding");
                    action=CoreIntegrityPolicy.Action.ESCROW_RELEASE;
                }
            }
            if(action==CoreIntegrityPolicy.Action.HEALTHY && placed && physical && rec!=null) {
                // Backfill orientation on v0.5.4 saves without optional CoreAxis.
                protections.setCoreAxis(team,level.getBlockState(pos).getValue(CoreKineticBlock.AXIS).getName());
            }
            if(action==CoreIntegrityPolicy.Action.RESTORE_AIR){
                try {
                    Direction.Axis axis=Direction.Axis.X;
                    if(rec!=null) axis=switch(rec.coreAxis){
                        case "y" -> Direction.Axis.Y;case "z" -> Direction.Axis.Z;default -> Direction.Axis.X;
                    };
                    var state=BaseCoreRegistries.CORE_BLOCK.get().defaultBlockState()
                        .setValue(CoreKineticBlock.AXIS,axis);
                    if(level.getBlockState(pos).isAir() && level.setBlock(pos,state,3)
                        && level.getBlockState(pos).is(BaseCoreRegistries.CORE_BLOCK.get())) {
                        level.invalidateCapabilities(pos);
                        if(level.getBlockEntity(pos) instanceof CoreKineticBlockEntity kinetic)
                            kinetic.refreshEngine();
                        RoglerTeamWarsLogger.warn("CORE_RESTORED_AFTER_EXTERNAL_REMOVAL team="+team
                            +" pos="+pos+"; third-party mod bypassed normal protection hooks");
                        notifyOwner(server,team,"Der Basis-Kern wurde nach einer externen Blockzerstoerung wiederhergestellt."
                            +" Die Umgebung ist moeglicherweise beschaedigt; pruefe deine Basis.");
                        continue;
                    }
                } catch(RuntimeException ex) {
                    RoglerTeamWarsLogger.warn("CORE_RESTORE_FAILED team="+team+" "+ex.getMessage());
                }
                action=CoreIntegrityPolicy.Action.ESCROW_RELEASE;
            }
            if(action==CoreIntegrityPolicy.Action.ESCROW_RELEASE){
                // Do not mutate the world/block or issue a replacement item. This is a
                // persisted, explicit recovery state with a revoked placed location.
                try {
                    db.ledger().rescueMissingPlaced(team,entry.token(),entry.location());
                    db.changed();
                    RoglerTeamWarsLogger.warn("CORE_QUARANTINED_MISSING_BLOCK team="+team
                        +" at="+pos+"; team may recover via /teamwars base recover");
                    notifyOwner(server,team,"Dein Basis-Kern fehlt nach einer externen Weltenaenderung."
                        +" Er liegt sicher im Recovery-Status. /teamwars base recover");
                } catch(IllegalStateException ex){
                    RoglerTeamWarsLogger.warn("CORE_QUARANTINE_FAILED team="+team+" "+ex.getMessage());
                }
                recoveryPolicy.resolved(team);
                if(rec!=null) FtbClaimBridge.release(level,team,rec,opSource(server,pos),protections);
            } else if(action==CoreIntegrityPolicy.Action.RELEASE_STALE_CLAIMS) {
                // Repeated retries if FTB is temporarily unavailable. Never unclaim a
                // manually placed chunk or a chunk now owned by a different team.
                FtbClaimBridge.release(level,team,rec,opSource(server,BlockPos.ZERO),protections);
            }
        }
    }
    private static void notifyOwner(MinecraftServer server,UUID team,String message) {
        for(ServerPlayer p:server.getPlayerList().getPlayers()) {
            var a=RoglerTeamWarsMod.assignmentFor(p);
            if(a.validParty() && team.equals(a.partyId()))
                p.sendSystemMessage(Component.literal("[TeamWars] "+message));
        }
    }
    /** OP check with count only; detailed positions are deliberately not sent to normal players. */
    public static int integrityAudit(CommandSourceStack src) {
        // Status-only by design: never changes physical blocks or FTB claims.
        int live=0,escrow=0,pending=0,missingLoaded=0,unloaded=0;
        var protections=BaseProtectionSavedData.get(src.getServer());
        for(var e:cores(src.getServer()).ledger().all()) {
            if(e.mode()==BaseCoreLedger.Mode.PLACED) {
                live++;
                if (e.location()!=null) {
                    var at=e.location();
                    BlockPos p=new BlockPos(at.x(),at.y(),at.z());
                    if (!src.getServer().overworld().hasChunkAt(p)) unloaded++;
                    else if(!src.getServer().overworld().getBlockState(p).is(BaseCoreRegistries.CORE_BLOCK.get())) missingLoaded++;
                }
            }
            if(e.mode()==BaseCoreLedger.Mode.ESCROW)escrow++;
            if(e.mode()!=BaseCoreLedger.Mode.PLACED) {
                var rec=protections.team(e.teamId());
                if(rec!=null)pending+=rec.createdClaims.size();
            }
        }
        final int a=live,b=escrow,c=pending,m=missingLoaded,u=unloaded;
        src.sendSuccess(()->Component.literal("[Rogler Core] READ-ONLY core audit: placed="+a
            +" escrow="+b+" loaded_missing="+m+" unloaded_unchecked="+u
            +" stale_auto_claim_receipts="+c
            +" (use /rogler admin corerepair apply to run a reconciliation pass)"),false);
        return Command.SINGLE_SUCCESS;
    }
    public static int integrityRepair(CommandSourceStack src) {
        verifyPhysicalCores(src.getServer());
        src.sendSuccess(()->Component.literal("[Rogler Core] Recovery pass executed for loaded core chunks;"
            +" restoration may be deferred by the five-second stability window. Review /rogler admin coreaudit."),false);
        return Command.SINGLE_SUCCESS;
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
                if(server.overworld().hasChunkAt(at) && !server.overworld().getBlockState(at)
                    .is(BaseCoreRegistries.CORE_BLOCK.get())) continue; // watchdog first; no ghost reclaims
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
        var infrastructure=dev.rogler.teamwars.neoforge.base.CoreInfrastructureSavedData.get(server);
        if(!infrastructure.canTransfer(oldTeam,newTeam)) {
            src.sendFailure(Component.literal("[TeamWars] Target team has existing core inventory/energy. Adoption refused to avoid item loss."));
            return 0;
        }
        try {
            // Transfer protection data first; pure ledger transfer cannot fail after preflight.
            protection.transferOrphanRecord(oldTeam,newTeam);
            var updated=saved.ledger().transferOrphan(oldTeam,newTeam,UUID.randomUUID());
            infrastructure.transfer(oldTeam,newTeam);
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
