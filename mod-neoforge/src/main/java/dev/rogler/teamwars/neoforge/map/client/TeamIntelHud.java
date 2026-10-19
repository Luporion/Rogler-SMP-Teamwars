package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.TeamIntelPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.Comparator;
import java.util.Locale;

/**
 * Light-weight HUD companion to Xaero (NOT a second map). Shows teammates
 * only, with direction/distance and the base-zone distance. Native Xaero marker
 * and terrain integration are deliberately NOT claimed by this renderer.
 */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TeamIntelHud {
    private TeamIntelHud() {}
    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"team_intel_hud"),TeamIntelHud::render);
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker partialTick) {
        Minecraft mc=Minecraft.getInstance();
        var state=TeamIntelClientHandler.active();
        if(state==null || mc.player==null || mc.options.hideGui || mc.screen!=null) return;
        int px=mc.player.blockPosition().getX(), pz=mc.player.blockPosition().getZ();
        boolean overworld=state.dimension().equals("minecraft:overworld");
        int x=8,y=graphics.guiHeight()-59;
        if(overworld){
            int distance=(int)Math.round(Math.hypot((long)px-state.spawnX(),(long)pz-state.spawnZ()));
            boolean inside=distance>=state.minRadius()&&distance<=state.maxRadius();
            graphics.drawString(mc.font,String.format(Locale.ROOT,"Spawn %dm | Base %d..%d %s",
                distance,state.minRadius(),state.maxRadius(),inside?"INSIDE":"OUTSIDE"),x,y,inside?0x77ECA4:0xF4BB6F,true);
        }else {
            graphics.drawString(mc.font,"Base zone: Overworld only",x,y,0xAAAAAA,true);
        }
        y+=12;
        var nearby=state.teammates().stream()
            .sorted(Comparator.comparingDouble(p -> p.dimension().equals(state.dimension())
                ? Math.hypot((long)px-p.x(),(long)pz-p.z()) : Double.MAX_VALUE))
            .limit(3).toList();
        for(TeamIntelPolicy.Presence p:nearby){
            String label;
            if(p.dimension().equals(state.dimension())) {
                long dx=(long)p.x()-px, dz=(long)p.z()-pz;
                int d=(int)Math.round(Math.hypot(dx,dz));
                // World-cardinal indication rather than player-facing compass.
                String heading=Math.abs(dx)>Math.abs(dz)?(dx>=0?"E":"W"):(dz>=0?"S":"N");
                label=p.name()+" "+d+"m "+heading;
            }else label=p.name()+" [other dimension]";
            graphics.drawString(mc.font,label,x,y,0x77C8FF,true);
            y+=11;
        }
    }
}
