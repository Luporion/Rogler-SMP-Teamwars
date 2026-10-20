package dev.rogler.teamwars.core;

import java.util.Properties;

public final class HudPreferencesTests {
    private static int checks;
    private static void check(boolean b,String msg){checks++;if(!b)throw new AssertionError(msg);}
    public static void main(String[] args){
        var defaults=HudPreferences.DEFAULT;
        check(!defaults.enabled(),"HUD disabled by default");
        check(defaults.teammateMarkers(),"team markers on by default");
        check(defaults.baseMarker(),"base marker on by default");
        check(!defaults.ftbWaypointMirror(),"no uncontrolled F marker spam by default");
        check(defaults.anchor()==HudPreferences.Anchor.TOP_RIGHT,"out of chat by default");
        var p=HudPreferences.fromProperties(defaults.toProperties());
        check(defaults.equals(p),"settings survive roundtrip");
        check(p.withSection("teammates",false).teammates()==false,"separate teammate toggle");
        check(p.withSection("respawn",false).worldSpawn(),"independent spawn toggles");
        check(p.withOffset(-2,5000).offsetX()==0&&p.withOffset(-2,5000).offsetY()==4000,"clamped offset");
        Properties corrupted=defaults.toProperties();corrupted.setProperty("hud.enabled","banana");
        boolean failed=false;try{HudPreferences.fromProperties(corrupted);}catch(IllegalArgumentException e){failed=true;}
        check(failed,"invalid booleans not silently accepted");
        check(!p.withAnchor(HudPreferences.Anchor.BOTTOM_LEFT).enabled(),"switch anchor doesn't enable hud");
        check(p.withEnabled(true).enabled(),"explicit opt in");
        System.out.println("v0.7.5 HudPreferencesTests: "+checks+" checks OK");
    }
}
