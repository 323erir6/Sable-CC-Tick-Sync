package dev.sableccticksync;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.methods.PeripheralMethod;

/** Keeps synchronous NeoPeripheral native-body methods off CC worker threads. */
public final class PhysicsPeripheralDispatch {
    private PhysicsPeripheralDispatch() {}

    // No dependency on NeoPeripheral: the fix is dormant when it is absent.
    public static boolean requiresServerThread(Object target) {
        return target != null && target.getClass().getName().equals(
            "io.p2vman.neoperipheral.peripheral.SableEnginePeripheral");
    }

    public static MethodResult defer(PeripheralMethod method, Object target,
                                     ILuaContext context, IComputerAccess computer,
                                     IArguments arguments) throws LuaException {
        // CC normally closes argument tables when the worker-side call returns.
        arguments.escapes();
        return context.executeMainThreadTask(() -> {
            MethodResult result = method.apply(target, context, computer, arguments);
            if (result == null) return null;
            // The supported engine API has synchronous methods only. Fail explicitly
            // if a future API changes this, rather than silently losing a continuation.
            if (result.getCallback() != null) {
                throw new LuaException("Unsupported yielding Sable engine method");
            }
            return result.getResult();
        });
    }
}
