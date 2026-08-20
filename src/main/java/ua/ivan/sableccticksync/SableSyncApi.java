package ua.ivan.sableccticksync;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class SableSyncApi implements ILuaAPI {
    private static final String[] API_NAME = new String[]{"sableSync"};
    private static final String[] NO_NAMES = new String[0];

    private final int computerId;

    public SableSyncApi(IComputerSystem computer) {
        this.computerId = computer.getID();
        // Clear stale state if the same numeric computer ID is recreated.
        HighFrequencyController.removeComputer(this.computerId);
    }

    @Override
    public String[] getNames() {
        // Do not query Sable/world state here: this is evaluated by CC's computer
        // thread while the Lua machine is created. The main server thread updates
        // the cache before Computer.tick() is allowed to start the machine.
        return HighFrequencyController.isOnConstruction(this.computerId)
                ? API_NAME
                : NO_NAMES;
    }

    @LuaFunction
    public final boolean setHighFrequency(boolean enabled) throws LuaException {
        requireSableConstruction();
        HighFrequencyController.setEnabled(this.computerId, enabled);
        return enabled;
    }

    @LuaFunction
    public final boolean isHighFrequency() throws LuaException {
        requireSableConstruction();
        return HighFrequencyController.isEnabled(this.computerId);
    }

    @LuaFunction
    public final double getExtraFrequency() throws LuaException {
        requireSableConstruction();
        return HighFrequencyController.isEnabled(this.computerId)
                ? HighFrequencyController.EXTRA_FREQUENCY_HZ
                : 0.0;
    }

    @Override
    public void shutdown() {
        HighFrequencyController.disable(this.computerId);
    }

    private void requireSableConstruction() throws LuaException {
        if (!HighFrequencyController.isOnConstruction(this.computerId)) {
            HighFrequencyController.disable(this.computerId);
            throw new LuaException("sableSync is only available while this computer is on an active Sable construction");
        }
    }
}
