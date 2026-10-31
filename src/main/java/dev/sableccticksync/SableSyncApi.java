package dev.sableccticksync;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;

public final class SableSyncApi implements ILuaAPI {
    private static final String[] API_NAME = {"sableSync"};
    private final int computerId;

    public SableSyncApi(IComputerSystem computer) {
        computerId = computer.getID();
        HighFrequencyController.registerComputer(computerId);
    }

    @Override
    public String[] getNames() {
        return API_NAME;
    }

    @LuaFunction
    public final boolean isHighFrequency() {
        return HighFrequencyController.isAccelerated(computerId);
    }

    @LuaFunction
    public final double getPhysicsFrequency() {
        return HighFrequencyController.getPhysicsFrequency(computerId);
    }

    @Override
    public void shutdown() {
        HighFrequencyController.removeComputer(computerId);
    }
}
