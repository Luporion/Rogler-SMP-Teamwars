package dev.rogler.teamwars.core;

import java.util.Locale;
import java.util.Properties;

/** Immutable client-only preferences. No server settings or player coordinates are persisted. */
public record HudPreferences(boolean enabled, boolean worldSpawn, boolean respawn,
                             boolean baseDistance, boolean teammates,
                             boolean teammateMarkers, boolean baseMarker, boolean ftbWaypointMirror,
                             Anchor anchor, int offsetX, int offsetY) {
    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public static final HudPreferences DEFAULT = new HudPreferences(false,true,true,true,true,
        true,true,false,Anchor.TOP_RIGHT,12,24);
    public HudPreferences {
        if(anchor==null) throw new IllegalArgumentException("anchor");
        if(offsetX<0 || offsetX>4000 || offsetY<0 || offsetY>4000) throw new IllegalArgumentException("offset 0..4000");
    }
    public HudPreferences withEnabled(boolean x){return new HudPreferences(x,worldSpawn,respawn,baseDistance,teammates,teammateMarkers,baseMarker,ftbWaypointMirror,anchor,offsetX,offsetY);}
    public HudPreferences withSection(String section,boolean x){
        return new HudPreferences(enabled,
            section.equals("world_spawn")?x:worldSpawn,section.equals("respawn")?x:respawn,
            section.equals("base_distance")?x:baseDistance,section.equals("teammates")?x:teammates,
            section.equals("team_markers")?x:teammateMarkers,section.equals("base_marker")?x:baseMarker,
            section.equals("ftb_waypoints")?x:ftbWaypointMirror,anchor,offsetX,offsetY);
    }
    public HudPreferences withAnchor(Anchor x){return new HudPreferences(enabled,worldSpawn,respawn,baseDistance,teammates,teammateMarkers,baseMarker,ftbWaypointMirror,x,offsetX,offsetY);}
    public HudPreferences withOffset(int x,int y){return new HudPreferences(enabled,worldSpawn,respawn,baseDistance,teammates,teammateMarkers,baseMarker,ftbWaypointMirror,anchor,Math.clamp(x,0,4000),Math.clamp(y,0,4000));}
    public Properties toProperties(){
        Properties p=new Properties();
        p.setProperty("hud.enabled",Boolean.toString(enabled));
        p.setProperty("hud.world_spawn",Boolean.toString(worldSpawn));
        p.setProperty("hud.respawn",Boolean.toString(respawn));
        p.setProperty("hud.base_distance",Boolean.toString(baseDistance));
        p.setProperty("hud.teammates",Boolean.toString(teammates));
        p.setProperty("map.team_markers",Boolean.toString(teammateMarkers));
        p.setProperty("map.base_marker",Boolean.toString(baseMarker));
        p.setProperty("map.ftb_waypoints",Boolean.toString(ftbWaypointMirror));
        p.setProperty("hud.anchor",anchor.name());
        p.setProperty("hud.offset_x",Integer.toString(offsetX));
        p.setProperty("hud.offset_y",Integer.toString(offsetY));
        return p;
    }
    public static HudPreferences fromProperties(Properties p){
        HudPreferences d=DEFAULT;
        try{
            Anchor anchor=Anchor.valueOf(p.getProperty("hud.anchor",d.anchor.name()).trim().toUpperCase(Locale.ROOT));
            return new HudPreferences(bool(p,"hud.enabled",d.enabled),bool(p,"hud.world_spawn",d.worldSpawn),
                bool(p,"hud.respawn",d.respawn),bool(p,"hud.base_distance",d.baseDistance),
                bool(p,"hud.teammates",d.teammates),bool(p,"map.team_markers",d.teammateMarkers),
                bool(p,"map.base_marker",d.baseMarker),bool(p,"map.ftb_waypoints",d.ftbWaypointMirror),
                anchor,Integer.parseInt(p.getProperty("hud.offset_x",String.valueOf(d.offsetX))),
                Integer.parseInt(p.getProperty("hud.offset_y",String.valueOf(d.offsetY))));
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid Rogler HUD setting: "+e.getMessage(),e);}
    }
    private static boolean bool(Properties p,String key,boolean defaultValue){
        String v=p.getProperty(key,Boolean.toString(defaultValue));
        if(!v.equalsIgnoreCase("true")&&!v.equalsIgnoreCase("false"))throw new IllegalArgumentException(key+" must be true or false");
        return Boolean.parseBoolean(v);
    }
}
