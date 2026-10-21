package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.core.WaypointSyncPolicy;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
    private static final Map<Integer, Object> inserted = new HashMap<>();
    private static Map<Integer, Object> xaeroCollection;
    private static String lastDimension="";
    private static long ticks;
    private static boolean warned;
    private FtbXaeroWaypointMirror() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        if(!HudSettingsStore.get().ftbWaypointMirror()){clear();return;}
        if(mc.level==null || mc.player==null){ clear();return; }
        String dim=mc.level.dimension().location().toString();
        // A dimension change must not render previous-dimension waypoints.
        if(!lastDimension.isEmpty() && !lastDimension.equals(dim)) clear();
        if(++ticks%60!=0) return;
        try {
            // In the exact installed Xaero Minimap 26.4.2 JAR this API returns
            // Hashtable<Integer, Waypoint>, NOT Collection<Waypoint>.
            // Preserve the namespaced table's integer keys and never touch other mods.
            Class<?> xaeroManager=Class.forName("xaero.common.minimap.waypoints.WaypointsManager");
            Object table=xaeroManager.getMethod("getCustomWaypoints",String.class).invoke(null,NAMESPACE);
            if(!(table instanceof Map<?, ?> map)) {
                reportOnce("Xaero custom-waypoint table is not a Map; integration disabled");
                clear();return;
            }
            @SuppressWarnings("unchecked") Map<Integer,Object> own=(Map<Integer,Object>)map;
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
            clearInserted(own);
            int index=1;
            for(Object obj:replacement){
                own.put(index,obj);
                inserted.put(index,obj);
                index++;
            }
            lastDimension=dim;warned=false;
        } catch(ReflectiveOperationException | RuntimeException ex) {
            reportOnce("FTB -> Xaero waypoint reflection unavailable: " + ex.getClass().getSimpleName());
            clear();
        }
    }

    private record SourceWaypoint(int x,int y,int z,String name) {}

    public static Object ftbManager() throws ReflectiveOperationException {
        Class<?> apiClass=Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
        Object client=apiClass.getMethod("clientApi").invoke(null);
        Object result=client.getClass().getMethod("getWaypointManager").invoke(client);
        return result instanceof Optional<?> value ? value.orElse(null) : null;
    }
    /** Only actual locally stored private FTB points, not ephemeral team imports. */
    public static List<WaypointSyncPolicy.Point> readFtbPoints() throws ReflectiveOperationException {
        Object manager=ftbManager();if(manager==null)return List.of();
        Object raw=manager.getClass().getMethod("getAllWaypoints").invoke(manager);
        if(!(raw instanceof Collection<?> entries))return List.of();
        List<WaypointSyncPolicy.Point> result=new ArrayList<>();
        String dim=Minecraft.getInstance().level.dimension().location().toString();
        for(Object waypoint:entries){
            if(result.size()>=256)break;
            Class<?> type=waypoint.getClass();
            if((Boolean)type.getMethod("isDeathpoint").invoke(waypoint) ||
               (Boolean)type.getMethod("isHidden").invoke(waypoint) ||
               (Boolean)type.getMethod("isTransient").invoke(waypoint))continue;
            Object pos=type.getMethod("getPos").invoke(waypoint);
            int x=(int)pos.getClass().getMethod("getX").invoke(pos);
            int y=(int)pos.getClass().getMethod("getY").invoke(pos);
            int z=(int)pos.getClass().getMethod("getZ").invoke(pos);
            String name=String.valueOf(type.getMethod("getName").invoke(waypoint)).trim();
            if(name.length()>WaypointSyncPolicy.MAX_NAME)name=name.substring(0,WaypointSyncPolicy.MAX_NAME);
            if(name.isBlank())continue;
            int color=(int)type.getMethod("getColor").invoke(waypoint) & 0xffffff;
            try{result.add(new WaypointSyncPolicy.Point(dim,x,y,z,name,color));}
            catch(IllegalArgumentException ignored){}
        }
        return result;
    }

    private static List<SourceWaypoint> readFtb() throws ReflectiveOperationException {
        Object manager=ftbManager();if(manager==null)return List.of();
        Object raw=manager.getClass().getMethod("getAllWaypoints").invoke(manager);
        if(!(raw instanceof Collection<?> entries)) return List.of();
        List<SourceWaypoint> resultList=new ArrayList<>();
        for(Object waypoint:entries){
            if(resultList.size()>=256)break;
            Class<?> type=waypoint.getClass();
            if((Boolean)type.getMethod("isDeathpoint").invoke(waypoint) ||
               (Boolean)type.getMethod("isHidden").invoke(waypoint) ||
               (Boolean)type.getMethod("isTransient").invoke(waypoint))continue;
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
    /** Remove only entries still holding objects placed by this adapter. */
    private static void clearInserted(Map<Integer,Object> table){
        for(var e: inserted.entrySet()){
            if(table.get(e.getKey())==e.getValue()) table.remove(e.getKey());
        }
        inserted.clear();
    }
    public static void clear(){
        if(xaeroCollection!=null) clearInserted(xaeroCollection);
        else inserted.clear();
        xaeroCollection=null;lastDimension="";ticks=0;
    }
}
