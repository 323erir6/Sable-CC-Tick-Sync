package dev.sableccticksync.mixin;

import dan200.computercraft.core.computer.Computer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.sableccticksync.FastPeripheralCallContext;
import dev.sableccticksync.HighFrequencyController;

@Mixin(targets = "dan200.computercraft.core.computer.LuaContext", remap = false)
public abstract class LuaContextMixin {
    @Redirect(
        method = "issueMainThreadTask",
        at = @At(
            value = "INVOKE",
            target = "Ldan200/computercraft/core/computer/Computer;queueMainThread(Ljava/lang/Runnable;)Z",
            remap = false
        ),
        remap = false
    )
    private boolean sableCcTickSync$routePeripheralTask(Computer computer, Runnable task) {
        int computerId = computer.getID();
        if (FastPeripheralCallContext.isPhysicsFastCall()
            && HighFrequencyController.isAccelerated(computerId)) {
            // Fast mod peripherals are serviced from a bounded shared queue.
            // Tick-bound CC/Minecraft peripherals never enter this branch.
            if (HighFrequencyController.enqueueFastTask(computerId, task)) return true;
        }
        return computer.queueMainThread(task);
    }
}
