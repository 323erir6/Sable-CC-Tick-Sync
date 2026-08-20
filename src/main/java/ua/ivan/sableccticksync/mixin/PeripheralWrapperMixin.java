package ua.ivan.sableccticksync.mixin;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.methods.PeripheralMethod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.ivan.sableccticksync.FastPeripheralCallContext;

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
    private MethodResult sableCcTickSync$markSupportedPeripheralCall(
            PeripheralMethod method,
            Object target,
            ILuaContext context,
            IComputerAccess computer,
            IArguments arguments
    ) throws LuaException {
        String type = target instanceof IPeripheral peripheral ? peripheral.getType() : null;
        if (!FastPeripheralCallContext.isSupportedType(type)) {
            return method.apply(target, context, computer, arguments);
        }

        FastPeripheralCallContext.enter();
        try {
            return method.apply(target, context, computer, arguments);
        } finally {
            FastPeripheralCallContext.exit();
        }
    }
}
