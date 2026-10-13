package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BaseCoreRegistries {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RoglerTeamWarsMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RoglerTeamWarsMod.MOD_ID);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,RoglerTeamWarsMod.MOD_ID);

    public static final DeferredBlock<CoreKineticBlock> CORE_BLOCK = BLOCKS.registerBlock("base_core",
        p -> new CoreKineticBlock(p),
        BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).lightLevel(state -> 8).noOcclusion());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoreKineticBlockEntity>> CORE_BE =
        BLOCK_ENTITIES.register("base_core", () -> BlockEntityType.Builder.of(
            BaseCoreRegistries::createCoreEntity, CORE_BLOCK.get()).build(null));
    // DeferredRegister constructs CORE_BE before the registered BlockEntityType exists.
    // Look up its holder only when Minecraft actually creates the BlockEntity,
    // never inside CORE_BE's own field initializer (illegal Java self-reference).
    private static CoreKineticBlockEntity createCoreEntity(net.minecraft.core.BlockPos pos,
                                                            net.minecraft.world.level.block.state.BlockState state) {
        return new CoreKineticBlockEntity(CORE_BE.get(), pos, state);
    }

    public static final DeferredItem<BaseCoreItem> CORE_ITEM = ITEMS.register("base_core",
        () -> new BaseCoreItem(CORE_BLOCK.get(), new Item.Properties().stacksTo(1).fireResistant()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus);

    }
    public static void registerCreateStress() {
        BlockStressValues.CAPACITIES.register(CORE_BLOCK.get(), () -> 16.0);
    }
    private BaseCoreRegistries() {}
}
