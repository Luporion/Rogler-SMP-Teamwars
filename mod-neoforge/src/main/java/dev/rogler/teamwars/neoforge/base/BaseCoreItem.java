package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

/** Reject forged, expired, cross-team, out-of-zone and second placements server-side. */
public final class BaseCoreItem extends BlockItem {
    public BaseCoreItem(Block block, Properties properties) { super(block, properties); }

    public static ItemStack authorized(UUID team, UUID token) {
        ItemStack stack = new ItemStack(BaseCoreRegistries.CORE_ITEM.get());
        CompoundTag values = new CompoundTag();
        values.putUUID("Team", team); values.putUUID("Token", token);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(values));
        return stack;
    }
    public static UUID team(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        var raw = data.copyTag();
        return raw.hasUUID("Team") ? raw.getUUID("Team") : null;
    }
    public static UUID token(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        var raw = data.copyTag();
        return raw.hasUUID("Token") ? raw.getUUID("Token") : null;
    }
    public static BaseCoreLedger.Location position(ServerLevel level, BlockPos pos) {
        return new BaseCoreLedger.Location(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    /** Never let vanilla fire/lava/explosion damage silently erase the only team core. */
    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) { return false; }

    /** Native NeoForge hook, called for each dropped core entity tick. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level() instanceof ServerLevel) BaseCoreService.monitorDroppedCore(entity, false);
        return false;
    }

    /** Last-resort callback for removal by sources that bypass the regular tick. */
    @Override
    public void onDestroyed(ItemEntity entity, DamageSource source) {
        if (entity.level() instanceof ServerLevel) BaseCoreService.monitorDroppedCore(entity, true);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return super.place(context);
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.FAIL;
        UUID team = team(context.getItemInHand());
        UUID token = token(context.getItemInHand());
        var member = RoglerTeamWarsMod.assignmentFor(player);
        if (team == null || token == null || !member.validParty() || !team.equals(member.partyId())) {
            player.displayClientMessage(Component.literal("[TeamWars] Core item is not issued to your FTB team."), true);
            return InteractionResult.FAIL;
        }
        BaseCoreConfig config = RoglerTeamWarsMod.baseConfig();
        BlockPos target = context.getClickedPos();
        var data = BaseCoreSavedData.get(level.getServer(), config.zone(level.getServer()), config.carrySeconds());
        var reason = data.ledger().placementFailure(team, token, position(level, target));
        if (reason != BaseCoreLedger.PlacementFailure.NONE) {
            String explanation = switch (reason) {
                case TOO_CLOSE_TO_SPAWN -> "TOO CLOSE TO WORLD SPAWN (minimum " + config.innerRadius() + " blocks)";
                case TOO_FAR_FROM_SPAWN -> "OUTSIDE ALLOWED BASE ZONE (maximum " + config.outerRadius() + " blocks)";
                case WRONG_DIMENSION -> "BASE CORES ARE ONLY ALLOWED IN THE OVERWORLD";
                case TIME_EXPIRED -> "TRANSPORT TIMER EXPIRED";
                case TOKEN_MISMATCH -> "CORE TOKEN INVALID OR REVOKED";
                default -> "YOUR TEAM ALREADY HAS A CORE OR IT IS IN RECOVERY";
            };
            player.displayClientMessage(Component.literal("[TeamWars] " + explanation), true);
            return InteractionResult.FAIL;
        }
        // Keep bases separated by at least one non-overlapping guarded chunk square.
        int radius = RoglerTeamWarsMod.protectionConfig().claimRadiusChunks();
        for (var other : data.ledger().all()) {
            if (!other.teamId().equals(team) && other.mode() == BaseCoreLedger.Mode.PLACED
                && other.location() != null &&
                dev.rogler.teamwars.core.base.BaseProtectionPolicy.overlaps(
                    other.location().x(), other.location().z(), target.getX(), target.getZ(), radius)) {
                player.displayClientMessage(Component.literal("[TeamWars] Another team's protected base is too close."), true);
                return InteractionResult.FAIL;
            }
        }
        // Perform vanilla/NeoForge placement (includes placement callbacks) BEFORE acknowledging persistence.
        InteractionResult result = super.place(context);
        if (result.consumesAction() && level.getBlockState(target).is(BaseCoreRegistries.CORE_BLOCK.get())) {
            data.ledger().placed(team, token, position(level, target));
            data.changed();
            // Creative placement normally retains the stack. A Base Core is a unique
            // server-issued bearer token and must be consumed in ALL game modes.
            if (player.getAbilities().instabuild && !context.getItemInHand().isEmpty()) {
                context.getItemInHand().shrink(1);
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
            }
            dev.rogler.teamwars.neoforge.protection.BaseGuardService.placed(level, player, team, target);
        }
        if (!result.consumesAction()) {
            player.displayClientMessage(Component.literal("[TeamWars] Placement blocked: check surface, protected claim or world border."), true);
        }
        return result;
    }
}
