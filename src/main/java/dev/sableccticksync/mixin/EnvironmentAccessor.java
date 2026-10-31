package dev.sableccticksync.mixin;

import dan200.computercraft.core.computer.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Environment.class, remap = false)
public interface EnvironmentAccessor {
    @Invoker("tick")
    void sableCcTickSync$invokeTick();
}
