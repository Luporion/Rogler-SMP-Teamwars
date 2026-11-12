package dev.rogler.teamwars.neoforge.compat.ftbmanagement;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.rogler.teamwars.neoforge.teams.FtbTeamManagement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.ftb.mods.ftbteams.data.TeamManagerImpl", remap = false)
abstract class PartyCreationMixin {
    @Inject(method = "createParty(Ljava/util/UUID;Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;Ljava/lang/String;Ldev/ftb/mods/ftblibrary/icon/Color4I;)Ldev/ftb/mods/ftbteams/data/PartyTeam;",
        at = @At("HEAD"))
    private void roglerCheckPartyLimit(CallbackInfoReturnable<?> info) throws CommandSyntaxException {
        FtbTeamManagement.checkCreate(this);
    }
}
