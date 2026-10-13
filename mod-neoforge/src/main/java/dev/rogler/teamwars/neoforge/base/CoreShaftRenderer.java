package dev.rogler.teamwars.neoforge.base;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/** Create's own rotating shaft partial, not static texture animation. */
public class CoreShaftRenderer extends KineticBlockEntityRenderer<CoreKineticBlockEntity> {
    public CoreShaftRenderer(BlockEntityRendererProvider.Context ctx) {super(ctx);}
    @Override protected BlockState getRenderedBlockState(CoreKineticBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }
    @Override protected SuperByteBuffer getRotatedModel(CoreKineticBlockEntity be,BlockState state) {
        return CachedBuffers.partial(AllPartialModels.SHAFT,state);
    }
}
