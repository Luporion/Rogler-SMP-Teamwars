package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.HashMap;
import java.util.Map;

/** Only server-authorized teammate positions and own base, via Xaero's published
 * mod-owned custom-waypoint table. No personal Xaero files or enemy positions. */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,value=Dist.CLIENT)
public final class TeamXaeroMarkers {
    private static final String NAMESPACE="roglercore_team_navigation";
    private static Map<Integer,Object> owner;
    private static final Map<Integer,Object> inserted=new HashMap<>();
    private static String priorDimension="";
    private static long ticks;
    private static boolean warned;
    private TeamXaeroMarkers(){}

    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){clear();return;}
        String dimension=mc.level.dimension().location().toString();
        if(!priorDimension.isEmpty()&&!priorDimension.equals(dimension))clear();
        if(++ticks%20!=0)return;
        try{
            var raw=Class.forName("xaero.common.minimap.waypoints.WaypointsManager")
                .getMethod("getCustomWaypoints",String.class).invoke(null,NAMESPACE);
            if(!(raw instanceof Map<?,?> m)){clear();return;}
            @SuppressWarnings("unchecked") Map<Integer,Object> table=(Map<Integer,Object>)m;
            if(owner!=table){clear();owner=table;}
            var constructor=Class.forName("xaero.common.minimap.waypoints.Waypoint")
                .getConstructor(int.class,int.class,int.class,String.class,String.class,int.class);
            Map<Integer,Object> newEntries=new HashMap<>();
            var settings=HudSettingsStore.get();
            var teammates=TeamIntelClientHandler.active();
            var nav=TeamNavigationClient.active();
            if(settings.teammateMarkers()&&teammates!=null){
                int markerKey=1;
                for(var player:teammates.teammates()){
                    if(!dimension.equals(player.dimension()))continue;
                    int id=markerKey++;
                    // Use true Y to avoid inaccurate 3D distance and false near-player alerts.
                    Object marker=constructor.newInstance(player.x(),player.y(),player.z(),
                        player.name(),"T",9);
                    newEntries.put(id,marker);
                }
            }
            if(settings.baseMarker()&&nav!=null&&dimension.equals(nav.coreDim())){
                Object base=constructor.newInstance(nav.coreX(),nav.coreY(),nav.coreZ(),"Teambasis","B",10);
                newEntries.put(0,base);
            }
            removeInserted();
            for(var item:newEntries.entrySet()){
                // The API provides a per-mod table: avoid touching unexpected entries.
                if(owner.putIfAbsent(item.getKey(),item.getValue())==null)
                    inserted.put(item.getKey(),item.getValue());
            }
            priorDimension=dimension;warned=false;
        }catch(ReflectiveOperationException|RuntimeException ex){
            if(!warned){System.err.println("[Rogler Core] Xaero team marker API not available: "+ex);warned=true;}
            clear();
        }
    }
    private static void removeInserted(){
        if(owner!=null)for(var item:inserted.entrySet()){
            if(owner.get(item.getKey())==item.getValue())owner.remove(item.getKey());
        }
        inserted.clear();
    }
    public static void clear(){removeInserted();owner=null;priorDimension="";ticks=0;}
}
