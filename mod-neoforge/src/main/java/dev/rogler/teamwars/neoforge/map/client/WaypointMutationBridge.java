package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.util.Collection;
import java.util.List;

/** Only performs EXPLICIT, token-checked edits of ONE uniquely resolved PERSONAL waypoint.
 * No server packet, raw file deletion, or edit of temporary/team/death/third-party points.
 * Always run from the Minecraft client thread. */
final class WaypointMutationBridge {
    private WaypointMutationBridge() {}

    private static Object xaeroManager() throws ReflectiveOperationException {
        var t=Class.forName("xaero.common.XaeroMinimapSession");
        Object session=t.getMethod("getCurrentSession").invoke(null);
        return session==null?null:t.getMethod("getWaypointsManager").invoke(session);
    }
    private static List<?> xaeroList() throws ReflectiveOperationException {
        var manager=xaeroManager();if(manager==null)throw new IllegalStateException("Xaero nicht bereit");
        Object world=manager.getClass().getMethod("getCurrentWorld").invoke(manager);
        if(world==null)throw new IllegalStateException("Xaero-Welt nicht bereit");
        Object set=world.getClass().getMethod("getCurrentSet").invoke(world);
        if(set==null)throw new IllegalStateException("Xaero-Set nicht bereit");
        Object list=set.getClass().getMethod("getList").invoke(set);
        if(!(list instanceof List<?> found))throw new IllegalStateException("Xaero-Liste nicht bereit");
        return found;
    }
    private static boolean personalXaero(Object obj) throws ReflectiveOperationException {
        Class<?> t=obj.getClass();
        return !(Boolean)t.getMethod("isTemporary").invoke(obj) &&
               !(Boolean)t.getMethod("isServerWaypoint").invoke(obj) &&
               !(Boolean)t.getMethod("isThirdParty").invoke(obj) &&
               !(Boolean)t.getMethod("isDisabled").invoke(obj);
    }
    private static boolean personalFtb(Object obj) throws ReflectiveOperationException {
        Class<?> t=obj.getClass();var mc=Minecraft.getInstance();
        return mc.level!=null && mc.level.dimension().equals(t.getMethod("getDimension").invoke(obj)) &&
               !(Boolean)t.getMethod("isDeathpoint").invoke(obj) &&
               !(Boolean)t.getMethod("isTransient").invoke(obj) &&
               !(Boolean)t.getMethod("isHidden").invoke(obj);
    }
    private static Object findXaero(WaypointSyncPolicy.Point expected) throws ReflectiveOperationException {
        Object found=null;
        for(Object obj:xaeroList()) {
            if(!personalXaero(obj))continue;
            var t=obj.getClass();
            if((int)t.getMethod("getX").invoke(obj)!=expected.x() ||
               (int)t.getMethod("getY").invoke(obj)!=expected.y() ||
               (int)t.getMethod("getZ").invoke(obj)!=expected.z())continue;
            // Compare actual palette color; never identify a third-party point by coordinates alone.
            if(found!=null)throw new IllegalStateException("Xaero-Koordinaten mehrdeutig");
            found=obj;
        }
        if(found==null)throw new IllegalStateException("Xaero-Ziel fehlt");
        if(!String.valueOf(found.getClass().getMethod("getName").invoke(found)).trim().equals(expected.name()) ||
           XaeroWaypointPalette.rgb(found)!=expected.color())
            throw new IllegalStateException("Xaero-Ziel hat sich geaendert");
        return found;
    }
    private static Object ftbManager() throws ReflectiveOperationException {
        var manager=FtbXaeroWaypointMirror.ftbManager();
        if(manager==null)throw new IllegalStateException("FTB nicht bereit");
        return manager;
    }
    private static Object findFtb(Object manager, WaypointSyncPolicy.Point expected) throws ReflectiveOperationException {
        Object all=manager.getClass().getMethod("getAllWaypoints").invoke(manager);
        if(!(all instanceof Collection<?> entries))throw new IllegalStateException("FTB-Liste nicht bereit");
        Object found=null;
        for(Object obj:entries){
            if(!personalFtb(obj))continue;
            var t=obj.getClass();
            if(!new BlockPos(expected.x(),expected.y(),expected.z()).equals(t.getMethod("getPos").invoke(obj)))continue;
            if(found!=null)throw new IllegalStateException("FTB-Koordinaten mehrdeutig");
            found=obj;
        }
        if(found==null)throw new IllegalStateException("FTB-Ziel fehlt");
        var t=found.getClass();
        if(!String.valueOf(t.getMethod("getName").invoke(found)).trim().equals(expected.name()) ||
           (((int)t.getMethod("getColor").invoke(found))&0xffffff)!=expected.color())
            throw new IllegalStateException("FTB-Ziel hat sich geaendert");
        return found;
    }
    static void deleteXaero(WaypointSyncPolicy.Point expected) throws ReflectiveOperationException {
        Object object=findXaero(expected);
        List<?> list=xaeroList();
        if(!list.remove(object))throw new IllegalStateException("Xaero verweigert Entfernen");
        xaeroManager().getClass().getMethod("updateWaypoints").invoke(xaeroManager());
    }
    static void deleteFtb(WaypointSyncPolicy.Point expected) throws ReflectiveOperationException {
        Object manager=ftbManager(),object=findFtb(manager,expected);
        var iface=Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint");
        Object removed=manager.getClass().getMethod("removeWaypoint",iface).invoke(manager,object);
        if(!Boolean.TRUE.equals(removed))throw new IllegalStateException("FTB verweigert Entfernen");
    }
    static void moveXaero(WaypointSyncPolicy.Point expected,WaypointSyncPolicy.Point moved) throws ReflectiveOperationException {
        Object object=findXaero(expected);Class<?> t=object.getClass();
        // Methods are validated against Xaero Minimap 26.4.2.
        var setX=t.getMethod("setX",int.class);var setY=t.getMethod("setY",int.class);
        var setZ=t.getMethod("setZ",int.class);var update=xaeroManager().getClass().getMethod("updateWaypoints");
        setX.invoke(object,moved.x());setY.invoke(object,moved.y());setZ.invoke(object,moved.z());
        update.invoke(xaeroManager());
    }
    static void moveFtb(WaypointSyncPolicy.Point expected,WaypointSyncPolicy.Point moved) throws ReflectiveOperationException {
        // FTB API has no setPos(); create new first, then remove the VERIFIED original.
        // If old removal fails, remove our replacement to avoid multiplying waypoints.
        Object manager=ftbManager(),old=findFtb(manager,expected);
        var iface=Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint");
        var add=manager.getClass().getMethod("addWaypointAt",BlockPos.class,String.class);
        var remove=manager.getClass().getMethod("removeWaypoint",iface);
        Object created=add.invoke(manager,new BlockPos(moved.x(),moved.y(),moved.z()),expected.name());
        if(created==null)throw new IllegalStateException("FTB konnte Wegpunkt nicht erstellen");
        try {
            created.getClass().getMethod("setColor",int.class).invoke(created,expected.color());
            if(!Boolean.TRUE.equals(remove.invoke(manager,old)))throw new IllegalStateException("FTB konnte alten Wegpunkt nicht loeschen");
        } catch(ReflectiveOperationException | RuntimeException ex){
            try{remove.invoke(manager,created);}catch(Exception rollback){ex.addSuppressed(rollback);}
            throw ex;
        }
    }
}
