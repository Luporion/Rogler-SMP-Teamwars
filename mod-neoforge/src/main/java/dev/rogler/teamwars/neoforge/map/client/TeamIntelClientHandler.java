package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.TeamIntelClientState;
import dev.rogler.teamwars.neoforge.map.TeamIntelPayload;
import net.minecraft.client.Minecraft;

/** Per-connection volatile client state. Does not write Xaero tiles or files. */
public final class TeamIntelClientHandler {
    private static final TeamIntelClientState STATE = new TeamIntelClientState();
    private TeamIntelClientHandler() {}
    public static void receive(TeamIntelPayload packet) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level == null || mc.player == null) return;
        STATE.accept(new TeamIntelClientState.Snapshot(packet.sequence(),packet.dimension(),
            packet.spawnX(),packet.spawnZ(),packet.innerRadius(),packet.outerRadius(),packet.teammates()),
            mc.level.getGameTime());
    }
    public static TeamIntelClientState.Snapshot active() {
        Minecraft mc=Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        var snapshot = STATE.active(mc.level.getGameTime());
        return snapshot != null && snapshot.dimension().equals(mc.level.dimension().location().toString())
            ? snapshot : null;
    }
    public static void clear(){STATE.reset();}
}
