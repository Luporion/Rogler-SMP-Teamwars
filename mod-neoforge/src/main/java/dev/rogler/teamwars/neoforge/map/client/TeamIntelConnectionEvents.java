package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Zero retained team coordinates across servers / logins. */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,value=Dist.CLIENT)
public final class TeamIntelConnectionEvents {
    private TeamIntelConnectionEvents() {}
    @SubscribeEvent public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) { TeamIntelClientHandler.clear(); }
    @SubscribeEvent public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { TeamIntelClientHandler.clear(); }
}
