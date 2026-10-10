package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
        if (!data.ledger().canPlace(team, token, position(level, target))) {
            player.displayClientMessage(Component.literal("[TeamWars] Core already placed, expired, or outside configured base zone."), true);
            return InteractionResult.FAIL;
        }
        // Perform vanilla/NeoForge placement (includes placement callbacks) BEFORE acknowledging persistence.
        InteractionResult result = super.place(context);
        if (result.consumesAction() && level.getBlockState(target).is(BaseCoreRegistries.CORE_BLOCK.get())) {
            data.ledger().placed(team, token, position(level, target));
            data.changed();
        }
        return result;
    }
}
