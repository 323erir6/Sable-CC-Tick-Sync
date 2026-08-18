package ua.ivan.sableccticksync.mixin;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.ivan.sableccticksync.PhysicsComputerTicker;
import ua.ivan.sableccticksync.ServerComputerBridge;

@Mixin(value = ServerComputer.class, remap = false)
public abstract class ServerComputerMixin implements ServerComputerBridge {
    @Shadow
    @Final
    private Computer computer;

    @Redirect(
        method = "tickServer",
        at = @At(
            value = "INVOKE",
            target = "Ldan200/computercraft/core/computer/Computer;tick()V",
            remap = false
        ),
        remap = false
    )
    private void sableCcTickSync$redirectNormalComputerTick(Computer computer) {
        ServerComputer serverComputer = (ServerComputer) (Object) this;
        if (!PhysicsComputerTicker.usesPhysicsTicks(serverComputer)) {
            computer.tick();
        }
    }

    @Override
    @Unique
    public void sableCcTickSync$physicsTick() {
        computer.tick();
    }
}
