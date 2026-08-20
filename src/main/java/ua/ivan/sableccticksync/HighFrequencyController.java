package ua.ivan.sableccticksync;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class HighFrequencyController {
    public static final double EXTRA_FREQUENCY_HZ = 100.0;

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final Set<Integer> ON_CONSTRUCTION = new HashSet<>();

    private HighFrequencyController() {
    }

    /**
     * Called only from the Minecraft server thread. Lua/computer threads must only
     * read the cached value through isOnConstruction().
     */
    public static synchronized void setOnConstruction(int computerId, boolean onConstruction) {
        if (onConstruction) {
            ON_CONSTRUCTION.add(computerId);
        } else {
            ON_CONSTRUCTION.remove(computerId);
            STATES.remove(computerId);
        }
    }

    public static synchronized boolean isOnConstruction(int computerId) {
        return ON_CONSTRUCTION.contains(computerId);
    }

    public static synchronized void setEnabled(int computerId, boolean enabled) {
        if (!enabled) {
            STATES.remove(computerId);
            return;
        }
        if (!ON_CONSTRUCTION.contains(computerId)) {
            STATES.remove(computerId);
            return;
        }

        State state = STATES.computeIfAbsent(computerId, ignored -> new State());
        state.enabled = true;
        state.fractionalTicks = 0.0;
    }

    public static synchronized boolean isEnabled(int computerId) {
        State state = STATES.get(computerId);
        return ON_CONSTRUCTION.contains(computerId) && state != null && state.enabled;
    }

    public static synchronized int consumeExtraTicks(int computerId, double physicsTimeStepSeconds) {
        if (!ON_CONSTRUCTION.contains(computerId)) return 0;

        State state = STATES.get(computerId);
        if (state == null || !state.enabled) return 0;
        if (!Double.isFinite(physicsTimeStepSeconds) || physicsTimeStepSeconds <= 0.0) return 0;

        state.fractionalTicks += EXTRA_FREQUENCY_HZ * physicsTimeStepSeconds;
        int wholeTicks = (int) Math.floor(state.fractionalTicks + 1.0E-12);
        state.fractionalTicks -= wholeTicks;
        return Math.max(0, wholeTicks);
    }

    public static synchronized void disable(int computerId) {
        STATES.remove(computerId);
    }

    public static synchronized void removeComputer(int computerId) {
        STATES.remove(computerId);
        ON_CONSTRUCTION.remove(computerId);
        FastPeripheralScheduler.clearComputer(computerId);
    }

    private static final class State {
        private boolean enabled;
        private double fractionalTicks;
    }
}
