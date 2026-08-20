package ua.ivan.sableccticksync.mixin;

import dan200.computercraft.core.computer.Computer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.ivan.sableccticksync.FastPeripheralCallContext;
import ua.ivan.sableccticksync.FastPeripheralScheduler;
import ua.ivan.sableccticksync.HighFrequencyController;

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
    private boolean sableCcTickSync$routeSupportedPeripheralTask(Computer computer, Runnable task) {
        int computerId = computer.getID();

        if (FastPeripheralCallContext.isActive()
                && HighFrequencyController.isOnConstruction(computerId)
                && HighFrequencyController.isEnabled(computerId)) {
            return FastPeripheralScheduler.enqueue(computerId, task);
        }

        return computer.queueMainThread(task);
    }
}
