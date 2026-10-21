package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Client-only, opt-in, non-destructive private FTB <-> Xaero import. Never uploads originals. */
public final class LocalWaypointBridge {
    private LocalWaypointBridge(){}
    private static Object xaeroManager() throws ReflectiveOperationException {
        var type=Class.forName("xaero.common.XaeroMinimapSession");
        Object session=type.getMethod("getCurrentSession").invoke(null);
        return session==null?null:type.getMethod("getWaypointsManager").invoke(session);
    }
    private static Object xaeroList() throws ReflectiveOperationException {
        Object manager=xaeroManager();if(manager==null)return null;
        Object world=manager.getClass().getMethod("getCurrentWorld").invoke(manager);
        if(world==null)return null;
        Object set=world.getClass().getMethod("getCurrentSet").invoke(world);
        return set==null?null:set.getClass().getMethod("getList").invoke(set);
    }
    public static List<WaypointSyncPolicy.Point> xaeroPoints() throws ReflectiveOperationException {
        Object raw=xaeroList();if(!(raw instanceof List<?> list))return List.of();
        List<WaypointSyncPolicy.Point> result=new ArrayList<>();
        var mc=Minecraft.getInstance();if(mc.level==null)return List.of();
        String dim=mc.level.dimension().location().toString();
        for(Object obj:list){
            if(result.size()>=256)break;
            var type=obj.getClass();
            if((Boolean)type.getMethod("isTemporary").invoke(obj) ||
               (Boolean)type.getMethod("isServerWaypoint").invoke(obj) ||
               (Boolean)type.getMethod("isThirdParty").invoke(obj) ||
               (Boolean)type.getMethod("isDisabled").invoke(obj))continue;
            int x=(int)type.getMethod("getX").invoke(obj),y=(int)type.getMethod("getY").invoke(obj),z=(int)type.getMethod("getZ").invoke(obj);
            String name=String.valueOf(type.getMethod("getName").invoke(obj)).trim();
            if(name.length()>WaypointSyncPolicy.MAX_NAME)name=name.substring(0,WaypointSyncPolicy.MAX_NAME);
            if(name.isEmpty())continue;
            // Xaero's getColor() is a PALETTE INDEX, not a 24-bit RGB color.
            int xaeroColor=(int)type.getMethod("getColor").invoke(obj);
            int color=(xaeroColor>=0 && xaeroColor<16)? 0xffffff : xaeroColor & 0xffffff;
            try{result.add(new WaypointSyncPolicy.Point(dim,x,y,z,name,color));}catch(IllegalArgumentException ignored){}
        }
        return result;
    }
    /** Imports missing points in both directions. No update/delete is propagated automatically. */
    public static String syncPrivate() throws ReflectiveOperationException {
        var mc=Minecraft.getInstance();if(mc.level==null)return "Not in a world";
        Object raw=xaeroList();Object ftb=FtbXaeroWaypointMirror.ftbManager();
        if(!(raw instanceof List<?> list)||ftb==null)return "Xaero/FTB manager is not ready";
        var fromXaero=xaeroPoints();var fromFtb=FtbXaeroWaypointMirror.readFtbPoints();
        var ftbMissing=WaypointSyncPolicy.missing(fromXaero,fromFtb);
        var xaeroMissing=WaypointSyncPolicy.missing(fromFtb,fromXaero);
        Method createFtb=ftb.getClass().getMethod("addWaypointAt",BlockPos.class,String.class);
        for(var p:ftbMissing){
            Object created=createFtb.invoke(ftb,new BlockPos(p.x(),p.y(),p.z()),p.name());
            if(created!=null)created.getClass().getMethod("setColor",int.class).invoke(created,p.color());
        }
        @SuppressWarnings("unchecked") List<Object> xaeroWaypoints=(List<Object>)list;
        var ctor=Class.forName("xaero.common.minimap.waypoints.Waypoint")
            .getConstructor(int.class,int.class,int.class,String.class,String.class,int.class);
        for(var p:xaeroMissing){
            Object obj=ctor.newInstance(p.x(),p.y(),p.z(),p.name(),"W",Math.floorMod(p.color(),16));
            xaeroWaypoints.add(obj);
        }
        Object manager=xaeroManager();
        if(manager!=null)manager.getClass().getMethod("updateWaypoints").invoke(manager);
        return "Private Wegpunkte importiert: Xaero -> FTB " + ftbMissing.size() + ", FTB -> Xaero " + xaeroMissing.size()
            + ". Originale wurden nicht geloescht; Xaero-Speicherung bitte testen.";
    }
}
