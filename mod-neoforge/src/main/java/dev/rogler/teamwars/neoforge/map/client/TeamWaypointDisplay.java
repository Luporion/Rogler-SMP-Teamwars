package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Shared pins are ephemeral both in Xaero (separate custom namespace) and FTB (transient waypoint API). */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,value=Dist.CLIENT)
public final class TeamWaypointDisplay {
    private static final String NAMESPACE="roglercore_shared_waypoints";
    private static Map<Integer,Object> xaero;
    private static final Map<Integer,Object> OWN_XAERO=new HashMap<>();
    private static final Map<String,Object> OWN_FTB=new HashMap<>();
    private static final Map<String,dev.rogler.teamwars.core.WaypointSyncPolicy.Point> OWN_FTB_POINTS=new HashMap<>();
    private static Object ftbManager;
    private static String dimension="";
    private static int ticks;
    private static boolean warned;
    private TeamWaypointDisplay(){}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){clear();return;}
        String dim=mc.level.dimension().location().toString();
        if(!dimension.isEmpty()&&!dimension.equals(dim))clear();
        if(++ticks%40!=0)return;
        try{
            var table=Class.forName("xaero.common.minimap.waypoints.WaypointsManager")
                .getMethod("getCustomWaypoints",String.class).invoke(null,NAMESPACE);
            if(!(table instanceof Map<?,?> raw)){clear();return;}
            @SuppressWarnings("unchecked") Map<Integer,Object> own=(Map<Integer,Object>)raw;
            if(own!=xaero){clear();xaero=own;}
            Object manager=FtbXaeroWaypointMirror.ftbManager();
            if(manager!=ftbManager){clearFtb();ftbManager=manager;}
            var ctor=Class.forName("xaero.common.minimap.waypoints.Waypoint")
                .getConstructor(int.class,int.class,int.class,String.class,String.class,int.class);
            Map<Integer,Object> next=new HashMap<>();
            int id=1;
            var active=TeamWaypointClient.active();
            for(var shared:active){
                var p=shared.point();if(!dim.equals(p.dimension()))continue;
                // Never put shared pins in Xaero's persistent personal waypoints.
                next.put(id++,ctor.newInstance(p.x(),p.y(),p.z(),p.name(),"S",Math.floorMod(p.color(),16)));
            }
            removeXaero();
            for(var entry:next.entrySet())if(xaero.putIfAbsent(entry.getKey(),entry.getValue())==null)
                OWN_XAERO.put(entry.getKey(),entry.getValue());
            if(manager!=null)updateFtb(manager,active,dim);
            dimension=dim;warned=false;
        }catch(ReflectiveOperationException|RuntimeException ex){
            if(!warned){warned=true;System.err.println("[Rogler Core] Shared waypoint adapter: "+ex);}
            clear();
        }
    }
    private static void updateFtb(Object manager,java.util.List<dev.rogler.teamwars.core.WaypointSyncPolicy.Shared> shared,String dim) throws ReflectiveOperationException {
        Map<String,dev.rogler.teamwars.core.WaypointSyncPolicy.Point> desired=new HashMap<>();
        for(var pin:shared)if(dim.equals(pin.point().dimension()))desired.put(pin.id().toString(),pin.point());
        // Remove only the actual transient waypoint instances we inserted.
        Class<?> type=Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint");
        Method remove=manager.getClass().getMethod("removeWaypoint",type);
        for(var it=OWN_FTB.entrySet().iterator();it.hasNext();){var entry=it.next();
            if(!desired.containsKey(entry.getKey()) || !desired.get(entry.getKey()).equals(OWN_FTB_POINTS.get(entry.getKey()))){
                remove.invoke(manager,entry.getValue());OWN_FTB_POINTS.remove(entry.getKey());it.remove();}
        }
        Method add=manager.getClass().getMethod("addTransientWaypointAt",BlockPos.class,String.class);
        var existing=FtbXaeroWaypointMirror.readFtbPoints();
        for(var entry:desired.entrySet()){
            if(OWN_FTB.containsKey(entry.getKey()))continue;
            var p=entry.getValue();
            if(existing.stream().anyMatch(x->x.locationKey().equals(p.locationKey())))continue;
            Object created=add.invoke(manager,new BlockPos(p.x(),p.y(),p.z()),p.name());
            if(created!=null){created.getClass().getMethod("setColor",int.class).invoke(created,p.color());
                OWN_FTB.put(entry.getKey(),created);OWN_FTB_POINTS.put(entry.getKey(),p);}
        }
    }
    private static void clearFtb(){
        if(ftbManager!=null){try{
            Method remove=ftbManager.getClass().getMethod("removeWaypoint",Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint"));
            for(Object created:OWN_FTB.values())remove.invoke(ftbManager,created);
        }catch(Exception ignored){}}
        OWN_FTB.clear();OWN_FTB_POINTS.clear();ftbManager=null;
    }
    private static void removeXaero(){
        if(xaero!=null)for(var e:OWN_XAERO.entrySet())if(xaero.get(e.getKey())==e.getValue())xaero.remove(e.getKey());
        OWN_XAERO.clear();
    }
    public static void clear(){removeXaero();clearFtb();xaero=null;dimension="";ticks=0;}
}
