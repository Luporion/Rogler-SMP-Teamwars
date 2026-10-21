package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import dev.rogler.teamwars.neoforge.map.WaypointMutationPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;

/** Explicit per-waypoint team sharing; no bulk share or hidden automatic upload. */
public final class WaypointSettingsScreen extends Screen {
    private enum Page { FTB, XAERO, TEAM }
    private final Screen parent;
    private Page tab=Page.FTB;
    private int first;
    private String message="Private Wegpunkte werden niemals automatisch hochgeladen.";
    public WaypointSettingsScreen(Screen parent){super(Component.literal("Rogler Core - Wegpunkte"));this.parent=parent;}
    @Override protected void init(){super.init();rebuild();}
    private void button(String label,int x,int y,int w,Runnable action){
        addRenderableWidget(Button.builder(Component.literal(label),b->{action.run();rebuild();})
            .bounds(x,y,w,20).build());
    }
    private void rebuild(){
        clearWidgets();int w=Math.min(470,width-22),x=(width-w)/2,y=50;
        button("FTB",x,y,w/3-4,()->{tab=Page.FTB;first=0;});
        button("Xaero",x+w/3,y,w/3-4,()->{tab=Page.XAERO;first=0;});
        button("Team",x+2*w/3,y,w/3,()->{tab=Page.TEAM;first=0;});y+=27;
        button("Private FTB <-> Xaero importieren",x,y,w,()->{
            try{message=LocalWaypointBridge.syncPrivate();}
            catch(Exception ex){message="Import fehlgeschlagen: "+ex.getClass().getSimpleName();}
        });y+=31;
        try{
            var mc=net.minecraft.client.Minecraft.getInstance();
            List<WaypointSyncPolicy.Point> points = tab==Page.FTB?FtbXaeroWaypointMirror.readFtbPoints():
                tab==Page.XAERO?LocalWaypointBridge.xaeroPoints():List.of();
            var team=TeamWaypointClient.active();
            int size=tab==Page.TEAM?team.size():points.size();
            for(int index=first;index<Math.min(first+7,size);index++){
                final int i=index;final int rowY=y;
                if(tab==Page.TEAM){
                    var shared=team.get(i);
                    if(mc.player!=null&&mc.player.getUUID().equals(shared.owner()))
                        button("Zuruecknehmen",x+w-128,rowY,128,()->{
                            PacketDistributor.sendToServer(new WaypointMutationPayload(true,shared.id(),null));
                            message="Widerruf gesendet: "+shared.point().name();
                        });
                }else{
                    String source=tab==Page.FTB?"ftb":"xaero";
                    button("Mit Team teilen",x+w-128,rowY,128,()->{
                        WaypointClientCommands.share(source,i);
                        message="Freigabe gesendet: "+points.get(i).name();
                    });
                }
                y+=23;
            }
            int buttonsY=Math.min(height-55,290);
            button("< Vorherige",x,buttonsY,w/3-5,()->first=Math.max(0,first-7));
            button("Naechste >",x+w/3,buttonsY,w/3-5,()->first=Math.min(Math.max(0,size-1)/7*7,first+7));
            button("Zurueck",x+2*w/3,buttonsY,w/3,()->{if(minecraft!=null)minecraft.setScreen(parent);});
        }catch(Exception ex){
            message="Karten-API noch nicht bereit: "+ex.getClass().getSimpleName();
            button("Zurueck",x,Math.min(height-55,290),w,()->{if(minecraft!=null)minecraft.setScreen(parent);});
        }
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        super.render(g,mouseX,mouseY,partial);
        g.drawCenteredString(font,"Rogler Core - Wegpunkte (nur ausdrueckliche Freigaben)",width/2,22,0xe0e9ee);
        try{
            List<WaypointSyncPolicy.Point> points=tab==Page.FTB?FtbXaeroWaypointMirror.readFtbPoints():
                tab==Page.XAERO?LocalWaypointBridge.xaeroPoints():List.of();
            var team=TeamWaypointClient.active();int count=tab==Page.TEAM?team.size():points.size();
            for(int i=first;i<Math.min(first+7,count);i++){
                String name=tab==Page.TEAM?team.get(i).point().name():points.get(i).name();
                if(name.length()>30)name=name.substring(0,30)+"...";
                g.drawString(font,i+": "+name,(width-Math.min(470,width-22))/2+4,84+(i-first)*23,0xe3e3e3,false);
            }
        }catch(Exception ignored){}
        String line=message.length()>75?message.substring(0,75):message;
        g.drawCenteredString(font,line,width/2,height-24,0xb3c6e3);
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
}
