package dev.sableccticksync.mixin;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sableccticksync.HighFrequencyController;
import dev.sableccticksync.ComputerTickDomain;
import dev.sableccticksync.ServerComputerBridge;

@Mixin(value = ServerComputer.class, remap = false)
public abstract class ServerComputerMixin implements ServerComputerBridge {
    @Shadow
    @Final
    private Computer computer;

    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private void sableCcTickSync$registerAcceleratedComputer(CallbackInfo ci) {
        ServerComputer serverComputer = (ServerComputer) (Object) this;
        HighFrequencyController.registerComputer(serverComputer.getID());
    }

    @Inject(method = "close", at = @At("HEAD"), remap = false)
    private void sableCcTickSync$forgetClosedComputer(CallbackInfo ci) {
        ServerComputer serverComputer = (ServerComputer) (Object) this;
        HighFrequencyController.removeComputer(serverComputer.getID());
    }

    @Override
    @Unique
    public void sableCcTickSync$physicsTick(boolean includeMinecraftTickState) {
        ComputerTickDomain.tick(computer, includeMinecraftTickState);
    }
}
