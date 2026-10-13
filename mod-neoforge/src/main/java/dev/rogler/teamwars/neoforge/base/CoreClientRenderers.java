package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class CoreClientRenderers {
    private CoreClientRenderers() {}
    @SubscribeEvent public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BaseCoreRegistries.CORE_BE.get(), CoreShaftRenderer::new);
    }
}
