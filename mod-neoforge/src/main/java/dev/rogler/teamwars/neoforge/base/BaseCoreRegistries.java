package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BaseCoreRegistries {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RoglerTeamWarsMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RoglerTeamWarsMod.MOD_ID);

    public static final DeferredBlock<Block> CORE_BLOCK = BLOCKS.registerSimpleBlock("base_core",
        BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).lightLevel(state -> 8).noOcclusion());
    public static final DeferredItem<BaseCoreItem> CORE_ITEM = ITEMS.register("base_core",
        () -> new BaseCoreItem(CORE_BLOCK.get(), new Item.Properties().stacksTo(1)));

    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); }
    private BaseCoreRegistries() {}
}
