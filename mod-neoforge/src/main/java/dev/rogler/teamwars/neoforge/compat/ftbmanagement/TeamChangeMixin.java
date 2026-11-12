package dev.rogler.teamwars.neoforge.compat.ftbmanagement;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ftb.mods.ftbteams.data.AbstractTeam", remap = false)
abstract class TeamChangeMixin {
    @Inject(method = "onPlayerChangeTeam", at = @At("RETURN"))
    private void roglerRefreshAfterTeamChange(CallbackInfo info) {
        RoglerTeamWarsMod.teamMembershipChanged();
    }
}
