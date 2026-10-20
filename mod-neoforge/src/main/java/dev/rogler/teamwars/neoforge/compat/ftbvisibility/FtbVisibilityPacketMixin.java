package dev.rogler.teamwars.neoforge.compat.ftbvisibility;

import dev.rogler.teamwars.core.FtbMapVisibilityPolicy;
import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import dev.rogler.teamwars.neoforge.teams.FtbPartyReader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authoritative replacement for FTB Chunks 1.21.1 PlayerVisibilityPacket.syncToPlayers.
 * FTB's original routine allows OPs to see everyone and honors publicly visible
 * team positions. Rogler SMP does not. Cancellation FIRST means a reflection or
 * FTB failure never falls through to the privacy-leaking original routine.
 * Target must match installed FTB Chunks; fail loudly on missing hook.
 */
@Pseudo
@Mixin(targets="dev.ftb.mods.ftbchunks.net.PlayerVisibilityPacket",remap=false)
public abstract class FtbVisibilityPacketMixin {
    private static final FtbPartyReader ROGLER_FTB = new FtbPartyReader();
    @Inject(method="syncToPlayers(Ljava/util/List;)V",at=@At("HEAD"),cancellable=true,remap=false,require=1)
    private static void rogler$restrictVisibility(List<ServerPlayer> input, CallbackInfo ci) {
        ci.cancel();
        try {
            List<ServerPlayer> players = input;
            if (players == null) {
                var server=ServerLifecycleHooks.getCurrentServer();
                if(server==null) return;
                players=server.getPlayerList().getPlayers();
            }
            // Refresh the FTB effective party for every connected player, not a cached team name.
            Map<UUID, TeamAssignmentPolicy.Party> parties=new HashMap<>();
            List<UUID> ids=new ArrayList<>(players.size());
            for(ServerPlayer p:players){ids.add(p.getUUID());parties.put(p.getUUID(),ROGLER_FTB.partyFor(p.getUUID()));}
            Class<?> packet=Class.forName("dev.ftb.mods.ftbchunks.net.PlayerVisibilityPacket");
            Constructor<?> constructor=packet.getConstructor(List.class);
            for(ServerPlayer recipient:players){
                List<UUID> visible=FtbMapVisibilityPolicy.visible(recipient.getUUID(),ids,parties);
                var outgoing=(CustomPacketPayload)constructor.newInstance(visible);
                PacketDistributor.sendToPlayer(recipient,outgoing);
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // Never invoke FTB's original method after a failure: it can leak all OP coordinates.
            System.err.println("[Rogler Core] FTB_VISIBILITY_DENY: restricted sync failed; original packet suppressed: " + ex);
        }
    }
}
