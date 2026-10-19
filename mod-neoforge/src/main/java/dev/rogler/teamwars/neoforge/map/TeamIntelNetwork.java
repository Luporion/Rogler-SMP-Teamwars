package dev.rogler.teamwars.neoforge.map;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Network handler executes on the client main thread by NeoForge default. */
public final class TeamIntelNetwork {
    private TeamIntelNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
            TeamIntelPayload.TYPE,
            TeamIntelPayload.STREAM_CODEC,
            (payload,context) -> dev.rogler.teamwars.neoforge.map.client.TeamIntelClientHandler.receive(payload)
        );
    }
}
