package dev.sableccticksync.mixin;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.methods.PeripheralMethod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.sableccticksync.FastPeripheralCallContext;
import dev.sableccticksync.PhysicsPeripheralDispatch;

@Mixin(targets = "dan200.computercraft.core.apis.PeripheralAPI$PeripheralWrapper", remap = false)
public abstract class PeripheralWrapperMixin {
    @Redirect(
        method = "call",
        at = @At(
            value = "INVOKE",
            target = "Ldan200/computercraft/core/methods/PeripheralMethod;apply(Ljava/lang/Object;Ldan200/computercraft/api/lua/ILuaContext;Ldan200/computercraft/api/peripheral/IComputerAccess;Ldan200/computercraft/api/lua/IArguments;)Ldan200/computercraft/api/lua/MethodResult;",
            remap = false
        ),
        remap = false
    )
    private MethodResult sableCcTickSync$markPeripheralCall(
        PeripheralMethod method,
        Object target,
        ILuaContext context,
        IComputerAccess computer,
        IArguments arguments
    ) throws LuaException {
        if (!FastPeripheralCallContext.isSupportedPeripheral(target)) {
            return method.apply(target, context, computer, arguments);
        }

        boolean physicsFast = FastPeripheralCallContext.isPhysicsFastPeripheral(target);
        FastPeripheralCallContext.enter(physicsFast);
        try {
            if (PhysicsPeripheralDispatch.requiresServerThread(target)) {
                // Yield the Lua coroutine, not the worker thread. LuaContextMixin
                // routes this task to the bounded pre-physics server queue.
                return PhysicsPeripheralDispatch.defer(method, target, context, computer, arguments);
            }
            return method.apply(target, context, computer, arguments);
        } finally {
            FastPeripheralCallContext.exit(physicsFast);
        }
    }
}
