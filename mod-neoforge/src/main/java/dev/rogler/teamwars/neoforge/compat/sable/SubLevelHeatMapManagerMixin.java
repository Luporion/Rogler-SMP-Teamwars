package dev.rogler.teamwars.neoforge.compat.sable;

import dev.rogler.teamwars.neoforge.compat.SableRemovedSublevelGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** OPTIONAL targeted injection. The guard is enabled only for Sable 2.0.6,
 * on the server and without Destructive. No Sable type in the class signature.
 * A destructive or partially applied split is NEVER intercepted here.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager", remap = false)
public abstract class SubLevelHeatMapManagerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void roglercore$removedOwnerTick(CallbackInfo ci) {
        if (SableRemovedSublevelGuard.shouldCancelTick(this)) ci.cancel();
    }

    @Inject(method = "split", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void roglercore$preflightBeforeSplitMutations(CallbackInfo ci) {
        if (SableRemovedSublevelGuard.shouldCancelSplit(this)) ci.cancel();
    }
}
