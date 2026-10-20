package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only, local FTB -> Xaero Custom Waypoints adapter. The source FTB waypoints
 * are never sent to the Rogler server, so private waypoints stay private.
 * Only entries created by THIS adapter are ever removed; no Xaero personal
 * waypoint file, player-managed waypoint or FTB waypoint is edited.
 *
 * Fail closed if Xaero's optional published custom-waypoint API is changed.
 * Full native Xaero -> FTB editor integration is NOT supplied by this class.
 */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,value=Dist.CLIENT)
public final class FtbXaeroWaypointMirror {
    private static final String NAMESPACE = "roglercore_ftb_mirror";
    private static final List<Object> inserted = new ArrayList<>();
    private static Collection<Object> xaeroCollection;
    private static String lastDimension="";
    private static long ticks;
    private static boolean warned;
    private FtbXaeroWaypointMirror() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null){ clear();return; }
        if(++ticks%60!=0) return;
        String dim=mc.level.dimension().location().toString();
        try {
            // The documented Xaero API gives each mod its own custom-waypoint table.
            // Treat unexpected collection shapes as unsupported; never modify a foreign table.
            Class<?> xaeroManager=Class.forName("xaero.common.minimap.waypoints.WaypointsManager");
            Object table=xaeroManager.getMethod("getCustomWaypoints",String.class).invoke(null,NAMESPACE);
            if(!(table instanceof Collection<?> collection)) {
                reportOnce("Xaero custom-waypoint table is not a Collection; integration disabled");
                clear();return;
            }
            @SuppressWarnings("unchecked") Collection<Object> own=(Collection<Object>)collection;
            // Avoid accidental stale entries on a new world/realm connection.
            if(xaeroCollection!=own) {clear();xaeroCollection=own;}
            List<SourceWaypoint> waypoints=readFtb();
            // Build first. If a reflected method fails, leave user waypoints unchanged.
            Class<?> waypointClass=Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Constructor<?> ctor=waypointClass.getConstructor(int.class,int.class,int.class,String.class,String.class,int.class);
            List<Object> replacement=new ArrayList<>();
            Map<String,Boolean> unique=new HashMap<>();
            for(SourceWaypoint p:waypoints) {
                if(replacement.size()>=256) break;
                String key=p.x+","+p.y+","+p.z;
                if(unique.putIfAbsent(key,true)!=null)continue;
                replacement.add(ctor.newInstance(p.x,p.y,p.z,p.name,"F",7));
            }
            for(Object old:inserted)own.remove(old);
            inserted.clear();
            for(Object obj:replacement){own.add(obj);inserted.add(obj);}
            lastDimension=dim;warned=false;
        } catch(ReflectiveOperationException | RuntimeException ex) {
            reportOnce("FTB -> Xaero waypoint reflection unavailable: " + ex.getClass().getSimpleName());
            clear();
        }
    }

    private record SourceWaypoint(int x,int y,int z,String name) {}

    private static List<SourceWaypoint> readFtb() throws ReflectiveOperationException {
        Class<?> apiClass=Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
        Object client=apiClass.getMethod("clientApi").invoke(null);
        Object result=client.getClass().getMethod("getWaypointManager").invoke(client);
        if(!(result instanceof Optional<?> optional)||optional.isEmpty())return List.of();
        Object manager=optional.get();
        Object raw=manager.getClass().getMethod("getAllWaypoints").invoke(manager);
        if(!(raw instanceof Collection<?> entries)) return List.of();
        List<SourceWaypoint> resultList=new ArrayList<>();
        for(Object waypoint:entries){
            if(resultList.size()>=256)break;
            Class<?> type=waypoint.getClass();
            if((Boolean)type.getMethod("isDeathpoint").invoke(waypoint) ||
               (Boolean)type.getMethod("isHidden").invoke(waypoint))continue;
            Object pos=type.getMethod("getPos").invoke(waypoint);
            int x=(int)pos.getClass().getMethod("getX").invoke(pos);
            int y=(int)pos.getClass().getMethod("getY").invoke(pos);
            int z=(int)pos.getClass().getMethod("getZ").invoke(pos);
            String name=String.valueOf(type.getMethod("getName").invoke(waypoint));
            if(name.length()>64)name=name.substring(0,64);
            resultList.add(new SourceWaypoint(x,y,z,name));
        }
        return resultList;
    }

    private static void reportOnce(String message){
        if(!warned){warned=true;System.err.println("[Rogler Core] WAYPOINT_BRIDGE: "+message);}
    }
    public static void clear(){
        if(xaeroCollection!=null) for(Object old:inserted) xaeroCollection.remove(old);
        inserted.clear();xaeroCollection=null;lastDimension="";ticks=0;
    }
}
