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
                ? "INSIDE" : "OUTSIDE"));
        return Command.SINGLE_SUCCESS;
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
                        ? "OUTSIDE BASE ZONE"
                        : config.outerRadius() - dist <= 150 ? "BOUNDARY NEAR" : "BASE ZONE";
                    player.displayClientMessage(Component.literal("[TeamWars] " + border + " | spawn "
                        + Math.round(dist) + " blocks | core " + (secondsLeft / 60) + "m "
                        + (secondsLeft % 60) + "s"), true);
                }
                break;
            }
        }
    }
}
