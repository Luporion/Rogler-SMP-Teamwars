package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import dev.rogler.teamwars.core.WaypointReconcilePolicy;
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
            // Read the actual RGB via WaypointColor.getHex(), not getColor()'s palette ordinal.
            int color=XaeroWaypointPalette.rgb(obj);
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
        var ftbMissing=WaypointReconcilePolicy.safeMissing(fromXaero,fromFtb);
        var xaeroMissing=WaypointReconcilePolicy.safeMissing(fromFtb,fromXaero);
        Method createFtb=ftb.getClass().getMethod("addWaypointAt",BlockPos.class,String.class);
        for(var p:ftbMissing){
            Object created=createFtb.invoke(ftb,new BlockPos(p.x(),p.y(),p.z()),p.name());
            if(created!=null)created.getClass().getMethod("setColor",int.class).invoke(created,p.color());
        }
        @SuppressWarnings("unchecked") List<Object> xaeroWaypoints=(List<Object>)list;
        var ctor=Class.forName("xaero.common.minimap.waypoints.Waypoint")
            .getConstructor(int.class,int.class,int.class,String.class,String.class,int.class);
        for(var p:xaeroMissing){
            Object obj=ctor.newInstance(p.x(),p.y(),p.z(),p.name(),"W",XaeroWaypointPalette.index(p.color()));
            xaeroWaypoints.add(obj);
        }
        Object manager=xaeroManager();
        if(manager!=null)manager.getClass().getMethod("updateWaypoints").invoke(manager);
        var report=WaypointReconcilePolicy.compare(fromFtb,fromXaero);
        return "Sicherer Privatimport: Xaero -> FTB " + ftbMissing.size() + ", FTB -> Xaero " + xaeroMissing.size()
            + "; " + report.conflicts() + " Konflikte/Mehrdeutigkeiten uebersprungen. Originale unveraendert.";
    }

    /** Dry run only; reads two personal stores without any file or network mutation. */
    public static WaypointReconcilePolicy.Report preview() throws ReflectiveOperationException {
        return WaypointReconcilePolicy.compare(FtbXaeroWaypointMirror.readFtbPoints(),xaeroPoints());
    }

    /**
     * Explicit name/color resolution at the SAME coordinates only. Never moves or deletes.
     * Token binds both prior values and source side; rejects changes since preview.
     */
    public static String resolveMetadata(String source, int index, String token) throws ReflectiveOperationException {
        if(!source.equals("ftb") && !source.equals("xaero"))return "Quelle muss ftb oder xaero sein";
        var ftb=FtbXaeroWaypointMirror.readFtbPoints();var xaero=xaeroPoints();
        var from=source.equals("ftb")?ftb:xaero;
        var to=source.equals("ftb")?xaero:ftb;
        if(index<0||index>=from.size())return "Index existiert nicht";
        var selected=from.get(index);
        if(!WaypointReconcilePolicy.canReplaceMetadata(from,to,selected))
            return "Abbruch: Ziel fehlt, mehrfach vorhanden oder moeglicherweise verschoben";
        var target=to.stream().filter(p->p.locationKey().equals(selected.locationKey())).findFirst().orElseThrow();
        if(!WaypointReconcilePolicy.token(source,selected,target).equals(token))
            return "Abbruch: Wegpunkt seit Vorschau geaendert. /roglerwaypoints preview erneut ausfuehren";
        if(selected.name().equals(target.name())&&selected.color()==target.color())return "Bereits identisch";
        if(source.equals("ftb")) {
            Object raw=xaeroList();if(!(raw instanceof List<?> list))return "Xaero-Speicher nicht verfuegbar";
            Object match=null;int count=0;
            for(Object obj:list){
                var t=obj.getClass();
                if((Boolean)t.getMethod("isTemporary").invoke(obj)||
                   (Boolean)t.getMethod("isServerWaypoint").invoke(obj)||
                   (Boolean)t.getMethod("isThirdParty").invoke(obj)||
                   (Boolean)t.getMethod("isDisabled").invoke(obj))continue;
                if((int)t.getMethod("getX").invoke(obj)==selected.x()&&
                   (int)t.getMethod("getY").invoke(obj)==selected.y()&&
                   (int)t.getMethod("getZ").invoke(obj)==selected.z()){match=obj;count++;}
            }
            if(count!=1)return "Abbruch: Xaero-Ziel nicht eindeutig";
            int palette=XaeroWaypointPalette.index(selected.color()); // prepare before mutation
            Object manager=xaeroManager();if(manager==null)return "Xaero-Manager nicht bereit";
            var rename=match.getClass().getMethod("setName",String.class);
            var recolor=match.getClass().getMethod("setColor",int.class);
            var refresh=manager.getClass().getMethod("updateWaypoints");
            rename.invoke(match,selected.name());
            recolor.invoke(match,palette);
            refresh.invoke(manager);
        } else {
            Object manager=FtbXaeroWaypointMirror.ftbManager();if(manager==null)return "FTB nicht bereit";
            Object all=manager.getClass().getMethod("getAllWaypoints").invoke(manager);
            if(!(all instanceof java.util.Collection<?> collection))return "FTB-Speicher nicht verfuegbar";
            Object match=null;int count=0;
            var mc=Minecraft.getInstance(); if(mc.level==null)return "Nicht in einer Welt";
            for(Object obj:collection){
                var t=obj.getClass();
                if(!mc.level.dimension().equals(t.getMethod("getDimension").invoke(obj)))continue;
                if((Boolean)t.getMethod("isDeathpoint").invoke(obj)||
                   (Boolean)t.getMethod("isTransient").invoke(obj)||
                   (Boolean)t.getMethod("isHidden").invoke(obj))continue;
                Object pos=t.getMethod("getPos").invoke(obj);
                if(pos.equals(new BlockPos(selected.x(),selected.y(),selected.z()))){match=obj;count++;}
            }
            if(count!=1)return "Abbruch: FTB-Ziel nicht eindeutig";
            var rename=match.getClass().getMethod("setName",String.class);
            var recolor=match.getClass().getMethod("setColor",int.class);
            rename.invoke(match,selected.name());
            recolor.invoke(match,selected.color());
        }
        return "Name/Farbe von "+source+" uebernommen: "+selected.name()+". Keine Koordinaten oder Dateien geloescht.";
    }

}
