package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.HudPreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small dependency-free settings UI for the optional HUD and Xaero overlay. */
public final class HudSettingsScreen extends Screen {
    private final Screen parent;
    public HudSettingsScreen(Screen parent){super(Component.literal("Rogler Core - HUD / Xaero"));this.parent=parent;}
    @Override protected void init(){super.init();rebuild();}
    private void button(String title,int x,int y,int w,Runnable action){
        addRenderableWidget(Button.builder(Component.literal(title),b->{action.run();rebuild();})
            .bounds(x,y,w,20).build());
    }
    private void rebuild(){
        clearWidgets();
        var p=HudSettingsStore.get();
        int width=310,x=(this.width-width)/2, y=Math.max(34,(this.height-310)/2);
        button("HUD: "+on(p.enabled()),x,y,width,()->set(p.withEnabled(!p.enabled()))); y+=24;
        button("Worldspawn: "+on(p.worldSpawn()),x,y,152,()->set(p.withSection("world_spawn",!p.worldSpawn())));
        button("Respawnpoint: "+on(p.respawn()),x+158,y,152,()->set(p.withSection("respawn",!p.respawn()))); y+=24;
        button("Basisdistanz: "+on(p.baseDistance()),x,y,152,()->set(p.withSection("base_distance",!p.baseDistance())));
        button("Teammates: "+on(p.teammates()),x+158,y,152,()->set(p.withSection("teammates",!p.teammates())));y+=24;
        button("Xaero Team-Marker: "+on(p.teammateMarkers()),x,y,width,()->set(p.withSection("team_markers",!p.teammateMarkers())));y+=24;
        button("Xaero Basis-Marker: "+on(p.baseMarker()),x,y,width,()->set(p.withSection("base_marker",!p.baseMarker())));y+=24;
        button("FTB -> Xaero Wegpunkte (F): "+on(p.ftbWaypointMirror()),x,y,width,()->set(p.withSection("ftb_waypoints",!p.ftbWaypointMirror())));y+=24;
        button("HUD-Position: "+p.anchor(),x,y,width,()->set(p.withAnchor(HudPreferences.Anchor.values()[(p.anchor().ordinal()+1)%4])));y+=24;
        button("X -8",x,y,72,()->set(p.withOffset(p.offsetX()-8,p.offsetY())));
        button("X +8",x+78,y,72,()->set(p.withOffset(p.offsetX()+8,p.offsetY())));
        button("Y -8",x+158,y,72,()->set(p.withOffset(p.offsetX(),p.offsetY()-8)));
        button("Y +8",x+238,y,72,()->set(p.withOffset(p.offsetX(),p.offsetY()+8)));y+=25;
        button("Wegpunkte: Synchronisieren / Teilen",x,y,width,()->{if(minecraft!=null)minecraft.setScreen(new WaypointSettingsScreen(this));});y+=24;
        button("Zurueck",x,y,width,()->{if(minecraft!=null)minecraft.setScreen(parent);});
    }
    private void set(HudPreferences p){HudSettingsStore.update(p);}
    private static String on(boolean enabled){return enabled?"AN":"AUS";}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        super.render(g,mouseX,mouseY,partial);
        g.drawCenteredString(font,"Rogler Core - HUD & Kartenmarker",width/2,24,0xD7E3ED);
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
}
