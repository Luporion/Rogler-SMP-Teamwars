package dev.rogler.teamwars.neoforge.map;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Network handler executes on the client main thread by NeoForge default. */
public final class TeamIntelNetwork {
    private TeamIntelNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("3");
        registrar.playToServer(dev.rogler.teamwars.neoforge.ui.ControlCenterRequestPayload.TYPE,
            dev.rogler.teamwars.neoforge.ui.ControlCenterRequestPayload.STREAM_CODEC,
            dev.rogler.teamwars.neoforge.ui.ControlCenterService::handle);
        registrar.playToClient(dev.rogler.teamwars.neoforge.ui.ControlCenterStatePayload.TYPE,
            dev.rogler.teamwars.neoforge.ui.ControlCenterStatePayload.STREAM_CODEC,
            (payload,context)->context.enqueueWork(()->dev.rogler.teamwars.neoforge.ui.client.ControlCenterClient.onState(payload)));
        registrar.playToClient(
            TeamIntelPayload.TYPE,
            TeamIntelPayload.STREAM_CODEC,
            (payload,context) -> dev.rogler.teamwars.neoforge.map.client.TeamIntelClientHandler.receive(payload)
        );
        registrar.playToClient(
            TeamNavigationPayload.TYPE, TeamNavigationPayload.STREAM_CODEC,
            (payload,context) -> dev.rogler.teamwars.neoforge.map.client.TeamNavigationClient.receive(payload)
        );
        registrar.playToServer(WaypointMutationPayload.TYPE, WaypointMutationPayload.STREAM_CODEC, TeamWaypointService::handle);
        registrar.playToClient(WaypointSnapshotPayload.TYPE, WaypointSnapshotPayload.STREAM_CODEC,
            (payload,context) -> context.enqueueWork(() -> dev.rogler.teamwars.neoforge.map.client.TeamWaypointClient.receive(payload)));
    }
}
