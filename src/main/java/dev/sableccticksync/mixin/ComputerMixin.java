package dev.sableccticksync.mixin;

import dan200.computercraft.core.computer.Environment;
import dan200.computercraft.core.redstone.RedstoneState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.sableccticksync.ComputerTickDomain;

@Mixin(targets = "dan200.computercraft.core.computer.Computer", remap = false)
public abstract class ComputerMixin {
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Ldan200/computercraft/core/redstone/RedstoneState;pollInputChanged()Z",
            remap = false
        ),
        remap = false
    )
    private boolean sableCcTickSync$pollRedstoneAtTwentyTps(RedstoneState redstone) {
        return !ComputerTickDomain.isPhysicsOnlyTick() && redstone.pollInputChanged();
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Ldan200/computercraft/core/computer/Environment;tick()V",
            remap = false
        ),
        remap = false
    )
    private void sableCcTickSync$tickTimersAtTwentyTps(Environment environment) {
        if (!ComputerTickDomain.isPhysicsOnlyTick()) {
            ((EnvironmentAccessor) (Object) environment).sableCcTickSync$invokeTick();
        }
    }
}
