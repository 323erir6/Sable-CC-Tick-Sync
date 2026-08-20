package ua.ivan.sableccticksync;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class SableSyncApi implements ILuaAPI {
    private final IComputerSystem computer;
    private final int computerId;

    public SableSyncApi(IComputerSystem computer) {
        this.computer = computer;
        this.computerId = computer.getID();
    }

    @Override
    public String[] getNames() {
        return new String[]{"sableSync"};
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
        if (!PhysicsComputerTicker.usesPhysicsTicks(this.computer)) {
            HighFrequencyController.disable(this.computerId);
            throw new LuaException("sableSync is only available while this computer is on an active Sable construction");
        }
    }
}
