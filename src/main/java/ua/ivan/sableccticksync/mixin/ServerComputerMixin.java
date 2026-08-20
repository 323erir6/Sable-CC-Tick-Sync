package ua.ivan.sableccticksync.mixin;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.ivan.sableccticksync.FastPeripheralScheduler;
import ua.ivan.sableccticksync.HighFrequencyController;
import ua.ivan.sableccticksync.PhysicsComputerTicker;
import ua.ivan.sableccticksync.ServerComputerBridge;

@Mixin(value = ServerComputer.class, remap = false)
public abstract class ServerComputerMixin implements ServerComputerBridge {
    @Shadow
    @Final
    private Computer computer;

    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private void sableCcTickSync$refreshConstructionState(CallbackInfo ci) {
        // tickServer runs on Minecraft's server thread. Keep all access to Sable's
        // world/sublevel lookup here rather than on CC's computer thread.
        ServerComputer serverComputer = (ServerComputer) (Object) this;
        PhysicsComputerTicker.refreshConstructionState(serverComputer);

        // Fallback path: if a supported fast-lane peripheral task was queued just
        // after the final physics substep, do not leave it waiting forever.
        FastPeripheralScheduler.runPending(serverComputer.getID(), 64);
    }

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
        if (!HighFrequencyController.isOnConstruction(serverComputer.getID())) {
            computer.tick();
        }
    }

    @Override
    @Unique
    public void sableCcTickSync$physicsTick() {
        computer.tick();
    }
}
