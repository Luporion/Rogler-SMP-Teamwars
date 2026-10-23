package dev.rogler.teamwars.neoforge.map.client;


/** Adapter for Xaero Minimap 26.4.2: waypoint colors are palette ORDINALS, not RGB. */
public final class XaeroWaypointPalette {
    private XaeroWaypointPalette() {}
    public static int rgb(Object waypoint) throws ReflectiveOperationException {
        Object color=waypoint.getClass().getMethod("getWaypointColor").invoke(waypoint);
        return (int)color.getClass().getMethod("getHex").invoke(color) & 0xffffff;
    }
    public static int nearestRgb(int rgb) throws ReflectiveOperationException {
        Class<?> type=Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
        Object[] values=type.getEnumConstants();
        return (int)type.getMethod("getHex").invoke(values[index(rgb)]) & 0xffffff;
    }
    public static int index(int rgb) throws ReflectiveOperationException {
        Class<?> type=Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
        Object[] colors=type.getEnumConstants();
        int result=-1;long min=Long.MAX_VALUE;
        for(int i=0;i<colors.length;i++){
            int hex=(int)type.getMethod("getHex").invoke(colors[i]) & 0xffffff;
            long r=((rgb>>16)&255)-((hex>>16)&255),
                 g=((rgb>>8)&255)-((hex>>8)&255),b=(rgb&255)-(hex&255);
            long d=r*r+g*g+b*b;
            if(d<min){min=d;result=i;}
        }
        if(result<0)throw new IllegalStateException("Xaero palette is empty");
        return result;
    }
}
