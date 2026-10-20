package dev.rogler.teamwars.neoforge.compat.ftbvisibility;

import dev.rogler.teamwars.neoforge.map.client.TeamIntelClientHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Defence in depth: even OP/public/stale FTB lists cannot draw a non-teammate. */
@Pseudo
@Mixin(targets="dev.ftb.mods.ftbchunks.client.VisibleClientPlayers",remap=false)
public abstract class FtbVisibleClientPlayersMixin {
    @Inject(method="isPlayerVisible",at=@At("HEAD"),cancellable=true,remap=false,require=1)
    private static void rogler$clientTeamFilter(Player player,CallbackInfoReturnable<Boolean> cir) {
        if (!TeamIntelClientHandler.mayDisplay(player.getUUID())) cir.setReturnValue(false);
    }
}
