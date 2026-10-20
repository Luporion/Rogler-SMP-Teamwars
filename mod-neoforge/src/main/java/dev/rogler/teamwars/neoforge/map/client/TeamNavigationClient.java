package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.neoforge.map.TeamNavigationPayload;
import net.minecraft.client.Minecraft;

/** Ephemeral, connection-scoped navigation targets; 10s fail-closed expiry. */
public final class TeamNavigationClient {
    private static TeamNavigationPayload current;
    private static long tick=-1,seq=-1;
    private TeamNavigationClient(){}
    public static void receive(TeamNavigationPayload data){
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || data.sequence()<=seq)return;
        current=data;seq=data.sequence();tick=mc.level.getGameTime();
    }
    public static TeamNavigationPayload active(){
        var mc=Minecraft.getInstance();
        if(mc.level==null || current==null)return null;
        long now=mc.level.getGameTime();return now>=tick && now-tick<=200?current:null;
    }
    public static void clear(){current=null;tick=-1;seq=-1;}
}
