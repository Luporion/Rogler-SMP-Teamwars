package dev.rogler.teamwars.neoforge.compat.ftbmanagement;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.rogler.teamwars.neoforge.teams.FtbTeamManagement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.ftb.mods.ftbteams.data.PartyTeam", remap = false)
abstract class PartyMembershipMixin {
    @Inject(method = "join(Lnet/minecraft/server/level/ServerPlayer;Lcom/mojang/authlib/GameProfile;)I",
        at = @At("HEAD"))
    private void roglerCheckJoining(CallbackInfoReturnable<Integer> info) throws CommandSyntaxException {
        FtbTeamManagement.checkCapacity(this);
    }

    @Inject(method = "invite(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/Collection;)I",
        at = @At("HEAD"))
    private void roglerCheckInviting(CallbackInfoReturnable<Integer> info) throws CommandSyntaxException {
        FtbTeamManagement.checkCapacity(this);
    }
}
