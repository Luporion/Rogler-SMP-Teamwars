package dev.rogler.teamwars.neoforge.map;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Xaero's documented server-message fair-play control; disables entity radar for everyone,
 * ensuring no enemy player icons are leaked by client radar settings.
 * It deliberately does not publish any teammates' coordinates (yet).
 * The map/waypoints themselves remain enabled. Cave mode is also disabled by fair-play.
 */
public final class XaeroPrivacyService {
    private XaeroPrivacyService() {}
    // Official Xaero marker: section signs are transmitted literally in a system message.
    private static final String FAIR_PLAY = "\u00a7f\u00a7a\u00a7i\u00a7r\u00a7x\u00a7a\u00a7e\u00a7r\u00a7o";

    public static void apply(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(FAIR_PLAY));
    }
}
