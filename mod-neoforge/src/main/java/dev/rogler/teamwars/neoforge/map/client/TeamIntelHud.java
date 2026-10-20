package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.HudPreferences;
import dev.rogler.teamwars.core.TeamIntelPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Minimal native NeoForge HUD, hidden until each client enables it. */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TeamIntelHud {
    private TeamIntelHud() {}
    @SubscribeEvent public static void register(RegisterGuiLayersEvent e){
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(RoglerTeamWarsMod.MOD_ID,"team_intel_hud"),TeamIntelHud::render);
    }
    private record Line(String text,int color){}
    private static void render(GuiGraphics g,net.minecraft.client.DeltaTracker delta){
        Minecraft mc=Minecraft.getInstance();
        HudPreferences settings=HudSettingsStore.get();
        if(!settings.enabled()||mc.player==null||mc.options.hideGui||mc.screen!=null)return;
        var team=TeamIntelClientHandler.active();
        if(team==null)return;
        var nav=TeamNavigationClient.active();
        int px=mc.player.blockPosition().getX(),pz=mc.player.blockPosition().getZ();
        String dimension=mc.player.level().dimension().location().toString();
        List<Line> lines=new ArrayList<>();
        if(settings.worldSpawn()&&dimension.equals("minecraft:overworld"))
            lines.add(new Line("Worldspawn: "+distance(px,pz,team.spawnX(),team.spawnZ())+"m",0x72E6AB));
        if(settings.respawn()){
            if(nav==null||nav.respawnDim().isEmpty())lines.add(new Line("Respawn: nicht gesetzt",0xA0ACB6));
            else if(!dimension.equals(nav.respawnDim()))lines.add(new Line("Respawn: andere Dimension",0xA0ACB6));
            else lines.add(new Line("Respawn: "+distance(px,pz,nav.respawnX(),nav.respawnZ())+"m",0x72E6AB));
        }
        if(settings.baseDistance()){
            if(nav==null||nav.coreDim().isEmpty())lines.add(new Line("Basis: nicht platziert",0xA0ACB6));
            else if(!dimension.equals(nav.coreDim()))lines.add(new Line("Basis: andere Dimension",0xA0ACB6));
            else lines.add(new Line("Basis: "+distance(px,pz,nav.coreX(),nav.coreZ())+"m",0xF1CA71));
        }
        if(settings.teammates()){
            var nearby=team.teammates().stream().sorted(Comparator.comparingDouble(p ->p.dimension().equals(dimension)?
                Math.hypot((long)px-p.x(),(long)pz-p.z()):Double.MAX_VALUE)).limit(4).toList();
            for(TeamIntelPolicy.Presence t:nearby){
                if(!dimension.equals(t.dimension())){lines.add(new Line(t.name()+": andere Dimension",0x9DB1C5));continue;}
                long dx=(long)t.x()-px,dz=(long)t.z()-pz;
                String cardinal=Math.abs(dx)>Math.abs(dz)?(dx>=0?"O":"W"):(dz>=0?"S":"N");
                lines.add(new Line(t.name()+": "+distance(px,pz,t.x(),t.z())+"m "+cardinal,0x76C7F3));
            }
        }
        if(lines.isEmpty())return;
        int width=lines.stream().mapToInt(l->mc.font.width(l.text())).max().orElse(0)+12;
        int height=lines.size()*12+8;
        boolean right=settings.anchor()==HudPreferences.Anchor.TOP_RIGHT||settings.anchor()==HudPreferences.Anchor.BOTTOM_RIGHT;
        boolean bottom=settings.anchor()==HudPreferences.Anchor.BOTTOM_LEFT||settings.anchor()==HudPreferences.Anchor.BOTTOM_RIGHT;
        int x=right?g.guiWidth()-width-settings.offsetX():settings.offsetX();
        int y=bottom?g.guiHeight()-height-settings.offsetY():settings.offsetY();
        x=Math.clamp(x,0,Math.max(0,g.guiWidth()-width));
        y=Math.clamp(y,0,Math.max(0,g.guiHeight()-height));
        g.fill(x,y,x+width,y+height,0x990B141C);
        int lineY=y+4;
        for(Line line:lines){g.drawString(mc.font,line.text(),x+6,lineY,line.color(),false);lineY+=12;}
    }
    private static long distance(int x,int z,int tx,int tz){return Math.round(Math.hypot((long)x-tx,(long)z-tz));}
}
