package ua.ivan.sableccticksync;

import java.util.HashMap;
import java.util.Map;

public final class HighFrequencyController {
    public static final double EXTRA_FREQUENCY_HZ = 100.0;

    private static final Map<Integer, State> STATES = new HashMap<>();

    private HighFrequencyController() {
    }

    public static synchronized void setEnabled(int computerId, boolean enabled) {
        if (!enabled) {
            STATES.remove(computerId);
            return;
        }

        State state = STATES.computeIfAbsent(computerId, ignored -> new State());
        state.enabled = true;
        state.fractionalTicks = 0.0;
    }

    public static synchronized boolean isEnabled(int computerId) {
        State state = STATES.get(computerId);
        return state != null && state.enabled;
    }

    public static synchronized int consumeExtraTicks(int computerId, double physicsTimeStepSeconds) {
        State state = STATES.get(computerId);
        if (state == null || !state.enabled) {
            return 0;
        }
        if (!Double.isFinite(physicsTimeStepSeconds) || physicsTimeStepSeconds <= 0.0) {
            return 0;
        }

        state.fractionalTicks += EXTRA_FREQUENCY_HZ * physicsTimeStepSeconds;
        int wholeTicks = (int) Math.floor(state.fractionalTicks + 1.0E-12);
        state.fractionalTicks -= wholeTicks;
        return Math.max(0, wholeTicks);
    }

    public static synchronized void disable(int computerId) {
        STATES.remove(computerId);
    }

    private static final class State {
        private boolean enabled;
        private double fractionalTicks;
    }
}
