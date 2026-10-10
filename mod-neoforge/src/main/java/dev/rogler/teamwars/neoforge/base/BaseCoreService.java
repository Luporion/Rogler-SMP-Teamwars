package dev.rogler.teamwars.neoforge.base;

import com.mojang.brigadier.Command;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.FluidTags;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BaseCoreService {
    private BaseCoreService() {}
    private static BaseCoreSavedData store(MinecraftServer server) {
        BaseCoreConfig config = RoglerTeamWarsMod.baseConfig();
        return BaseCoreSavedData.get(server, config.zone(server), config.carrySeconds());
    }
    private static void say(CommandSourceStack source, String msg) {
        source.sendSuccess(() -> Component.literal("[TeamWars] " + msg), false);
    }
    public static int issue(CommandSourceStack source, ServerPlayer player, boolean recovery) {
        var assignment = RoglerTeamWarsMod.assignmentFor(player);
        if (!assignment.validParty()) { say(source,"Target player must be in a valid FTB party."); return 0; }
        if (player.getInventory().getFreeSlot() == -1) {
            say(source,"Inventory full; nothing issued."); return 0;
        }
        BaseCoreSavedData data = store(source.getServer());
        UUID team = assignment.partyId();
        UUID token = UUID.randomUUID();
        try {
            if (recovery) data.ledger().reissueFromEscrow(team, token);
            else data.ledger().issue(team, token);
            data.changed();
            if (!player.getInventory().add(BaseCoreItem.authorized(team, token))) {
                // Impossible with a free slot checked above; state persists and admin can handle if it occurs.
                say(source,"ERROR: issued in ledger, but inventory refused item. Inspect before recovery."); return 0;
            }
            say(source, (recovery ? "Recovered" : "Issued") + " core to " + player.getName().getString()
                + " | team " + team + " | token " + token);
            return Command.SINGLE_SUCCESS;
        } catch (IllegalStateException e) { say(source, e.getMessage()); return 0; }
    }
    /** Explicit operator repair for lost v0.2.0 ISSUED items; revokes even a still-existing token. */
    public static int reclaimLost(CommandSourceStack source, ServerPlayer player) {
        var assignment = RoglerTeamWarsMod.assignmentFor(player);
        if (!assignment.validParty()) { say(source, "Player needs a valid FTB party."); return 0; }
        BaseCoreSavedData data = store(source.getServer());
        var entry = data.ledger().get(assignment.partyId());
        if (entry == null || entry.mode() != BaseCoreLedger.Mode.ISSUED) {
            say(source, "Only an ISSUED (not PLACED/ESCROW) core can be reclaimed."); return 0;
        }
        if (!data.ledger().rescueToEscrow(entry.teamId(), entry.token(), null)) return 0;
        data.changed();
        say(source, "Core revoked and moved to recovery escrow. Any remaining old item is INVALID now."
            + " Team member can run /teamwars base recover.");
        RoglerTeamWarsMod.warn("OP explicitly reclaimed issued core for team " + entry.teamId());
        return Command.SINGLE_SUCCESS;
    }

    /** Normal players may only inspect their own FTB team's core, never enemy base locations. */
    public static int ownStatus(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var assignment = RoglerTeamWarsMod.assignmentFor(player);
        if (!assignment.validParty()) { say(source, "You are not in a valid FTB party."); return 0; }
        var entry = store(source.getServer()).ledger().get(assignment.partyId());
        if (entry == null) { say(source, "Your team has no issued base core. Ask an admin to issue one."); return 0; }
        say(source, "Your base core: " + entry.mode() + " | location: " + entry.location()
            + " | transport: " + entry.carriedSeconds() + "s | recovery: " + entry.lastSafe());
        return Command.SINGLE_SUCCESS;
    }
    public static int info(CommandSourceStack source) {
        var data = store(source.getServer());
        say(source, "Registered cores: " + data.ledger().size());
        for (var entry: data.ledger().all())
            say(source, entry.teamId() + " : " + entry.mode() + " @ " + entry.location()
                + " (carried " + entry.carriedSeconds() + "s)");
        return Command.SINGLE_SUCCESS;
    }
    public static int distance(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var zone = RoglerTeamWarsMod.baseConfig().zone(source.getServer());
        double d = zone.distance(player.blockPosition().getX(), player.blockPosition().getZ());
        say(source, "Worldspawn distance: " + Math.round(d) + " blocks | base allowed "
            + RoglerTeamWarsMod.baseConfig().innerRadius() + " - "
            + RoglerTeamWarsMod.baseConfig().outerRadius() + " | "
            + (zone.placementAllowed(player.blockPosition().getX(),player.blockPosition().getZ())
                ? "INSIDE ALLOWED BASE ZONE" : "OUTSIDE ALLOWED BASE ZONE"));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Rescue is called from the actual dropped item hooks. A valid server-issued token is required.
     * Do not scan inventories/chests and guess "missing": they can legitimately hold the core.
     * Placement uses the real player and BlockItem.place so normal permissions still run.
     * No online teammate close enough -> revoke old token to escrow (recoverable by the team).
     */
    public static void monitorDroppedCore(ItemEntity entity, boolean destroyedOrExpired) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        ItemStack stack = entity.getItem();
        if (!stack.is(BaseCoreRegistries.CORE_ITEM.get())) return;
        UUID team = BaseCoreItem.team(stack), token = BaseCoreItem.token(stack);
        if (team == null || token == null) return;
        BaseCoreSavedData saved = store(level.getServer());
        var entry = saved.ledger().get(team);
        if (entry == null || entry.mode() != BaseCoreLedger.Mode.ISSUED || !token.equals(entry.token())) {
            // Stale duplicates never grant extra bases; no mutation to the valid ledger.
            if (!destroyedOrExpired) entity.discard();
            return;
        }
        BlockPos source = entity.blockPosition();
        boolean hazard = destroyedOrExpired || entity.isInLava() || entity.isOnFire()
            || level.getFluidState(source).is(FluidTags.LAVA)
            || level.getBlockState(source).is(Blocks.CACTUS)
            || level.getBlockState(source).is(Blocks.FIRE)
            || level.getBlockState(source).is(Blocks.SOUL_FIRE)
            || entity.getY() < level.getMinBuildHeight() + 4
            || entity.tickCount >= 5800;
        if (!hazard) return;
        // Hazard coordinates are NOT a safe respawn position, even if inside the radial zone.
        var safe = entry.lastSafe();
        if (!destroyedOrExpired && tryPlaceNearDroppedCore(level, team, stack, source, entry.lastSafe())) {
            // BlockItem.place already changed the saved ledger, and consumed the item stack.
            entity.discard();
            notifyTeam(level.getServer(), team, "Dropped core automatically placed after environmental hazard.");
            return;
        }
        if (saved.ledger().rescueToEscrow(team, token, safe)) {
            saved.changed();
            notifyTeam(level.getServer(), team, "Core protected from destruction: safe placement unavailable."
                + " Use /teamwars base recover with one empty inventory slot.");
            RoglerTeamWarsMod.warn("Core hazard rescue entered escrow for team " + team
                + "; lastSafe=" + safe + "; destroyed=" + destroyedOrExpired);
        }
        if (!destroyedOrExpired) entity.discard();
    }

    private static void notifyTeam(MinecraftServer server, UUID team, String message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var assignment = RoglerTeamWarsMod.assignmentFor(player);
            if (assignment.validParty() && team.equals(assignment.partyId()))
                player.sendSystemMessage(Component.literal("[TeamWars] " + message));
        }
    }

    private static boolean tryPlaceNearDroppedCore(ServerLevel level, UUID team, ItemStack stack,
                                                    BlockPos dropped, BaseCoreLedger.Location fallback) {
        if (level != level.getServer().overworld()) return false;
        // A close-by real team member is needed so FTB/NeoForge permissions aren't circumvented.
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || player.distanceToSqr(Vec3.atCenterOf(dropped)) > 16 * 16) continue;
            var assignment = RoglerTeamWarsMod.assignmentFor(player);
            if (!assignment.validParty() || !team.equals(assignment.partyId())) continue;
            if (attemptPlacementNear(level, player, stack, dropped)) return true;
            if (fallback != null && "minecraft:overworld".equals(fallback.dimension())) {
                BlockPos anchor = new BlockPos(fallback.x(), fallback.y(), fallback.z());
                if (player.distanceToSqr(Vec3.atCenterOf(anchor)) <= 16 * 16
                    && attemptPlacementNear(level, player, stack, anchor)) return true;
            }
        }
        return false;
    }

    /** Does not change any block directly; attempts vanilla placement with the real player as actor. */
    private static boolean attemptPlacementNear(ServerLevel level, ServerPlayer player,
                                                ItemStack stack, BlockPos anchor) {
        var zone = RoglerTeamWarsMod.baseConfig().zone(level.getServer());
        for (int dy = 1; dy >= -2; dy--) for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos target = anchor.offset(dx, dy, dz);
                if (!zone.placementAllowed(target.getX(), target.getZ()) || !level.isLoaded(target)) continue;
                if (!level.getBlockState(target).isAir() || !level.getFluidState(target).isEmpty()) continue;
                BlockPos support = target.below();
                if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) continue;
                if (player.distanceToSqr(Vec3.atCenterOf(target)) > 16 * 16) continue;
                var hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
                var placement = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
                if (placement.getClickedPos().equals(target)
                    && ((BaseCoreItem) stack.getItem()).place(placement).consumesAction()) return true;
            }
        return false;
    }

    /** Uses actual vanilla BlockItem placement, including NeoForge/claim event hooks. Never calls setBlock directly. */
    private static boolean attemptSafeAutomaticPlacement(ServerPlayer player, ItemStack stack) {
        InteractionHand hand;
        if (player.getMainHandItem() == stack) hand = InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem() == stack) hand = InteractionHand.OFF_HAND;
        else return false;
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        var zone = RoglerTeamWarsMod.baseConfig().zone(level.getServer());
        BlockPos feet = player.blockPosition();
        // Find a dry, supported vacant block two steps from player's body (no forced overwrite).
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;
            BlockPos target = feet.offset(dx, 0, dz);
            if (!zone.placementAllowed(target.getX(), target.getZ())) continue;
            if (!level.getBlockState(target).isAir() || !level.getFluidState(target).isEmpty()) continue;
            if (!level.getBlockState(target.below()).isFaceSturdy(level, target.below(), Direction.UP)) continue;
            BlockPos clicked = target.below();
            var hit = new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false);
            var placement = new BlockPlaceContext(player, hand, stack, hit);
            if (((BaseCoreItem) stack.getItem()).place(placement).consumesAction()) return true;
        }
        return false;
    }

    public static void oneSecond(MinecraftServer server) {
        BaseCoreConfig config = RoglerTeamWarsMod.baseConfig();
        var saved = store(server);
        Set<UUID> seen = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var party = RoglerTeamWarsMod.assignmentFor(player);
            if (!party.validParty()) continue;
            UUID id = party.partyId();
            var entry = saved.ledger().get(id);
            if (entry == null || entry.mode() != BaseCoreLedger.Mode.ISSUED || seen.contains(id)) continue;
            for (int slot=0; slot<player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.is(BaseCoreRegistries.CORE_ITEM.get())) continue;
                if (!id.equals(BaseCoreItem.team(stack)) || !entry.token().equals(BaseCoreItem.token(stack))) continue;
                seen.add(id);
                var pos = BaseCoreItem.position((net.minecraft.server.level.ServerLevel) player.level(), player.blockPosition());
                var safe = saved.ledger().legal(pos) ? pos : null;
                if (entry.carriedSeconds() + 1 >= config.carrySeconds()
                    && attemptSafeAutomaticPlacement(player, stack)) {
                    player.sendSystemMessage(Component.literal("[TeamWars] The 1h transport deadline automatically placed your core."));
                    break;
                }
                var update = saved.ledger().advanceCarrier(id, entry.token(), 1, safe);
                saved.changed();
                if (update.mode() == BaseCoreLedger.Mode.ESCROW) {
                    // Fail closed: no forced setBlock bypassing FTB claims/placement events.
                    stack.shrink(1);
                    player.sendSystemMessage(Component.literal("[TeamWars] Core transport expired. Safe placement was impossible; core stored in admin escrow. Use /teamwars admin recovercore <player>."));
                    RoglerTeamWarsMod.warn("Core entered escrow at transport timeout for team " + id
                        + "; last in-zone location=" + update.lastSafe());
                } else {
                    double dist = config.zone(server).distance(pos.x(), pos.z());
                    long secondsLeft = config.carrySeconds() - update.carriedSeconds();
                    String border = !config.zone(server).placementAllowed(pos.x(), pos.z())
                        ? "OUTSIDE ALLOWED BASE ZONE"
                        : config.outerRadius() - dist <= 150 ? "ALLOWED BASE BORDER NEAR" : "INSIDE ALLOWED BASE ZONE";
                    player.displayClientMessage(Component.literal("[TeamWars] " + border + " | spawn "
                        + Math.round(dist) + " blocks | core " + (secondsLeft / 60) + "m "
                        + (secondsLeft % 60) + "s"), true);
                }
                break;
            }
        }
    }
}
