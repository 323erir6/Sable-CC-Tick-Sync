package dev.sableccticksync;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class HighFrequencyController {
    private static final int MAX_PENDING_FAST_TASKS = 4096;
    private static final int MAX_TASKS_PER_SUBSTEP = 1024;
    private static final long MAX_TASK_TIME_PER_SUBSTEP_NANOS = 2_000_000L;
    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final Set<Integer> ACCELERATED_COMPUTERS = new HashSet<>();
    private static final ConcurrentLinkedQueue<State> READY_COMPUTERS =
        new ConcurrentLinkedQueue<>();

    private HighFrequencyController() {
    }

    public static synchronized void registerComputer(int computerId) {
        ACCELERATED_COMPUTERS.add(computerId);
        STATES.computeIfAbsent(computerId, ignored -> new State());
    }

    public static synchronized boolean isAccelerated(int computerId) {
        return ACCELERATED_COMPUTERS.contains(computerId);
    }

    public static synchronized void recordPhysicsStep(int computerId, double timeStepSeconds) {
        if (!ACCELERATED_COMPUTERS.contains(computerId)) return;
        if (!Double.isFinite(timeStepSeconds) || timeStepSeconds <= 0.0) return;
        STATES.computeIfAbsent(computerId, ignored -> new State()).physicsTimeStepSeconds =
            timeStepSeconds;
    }

    public static synchronized double getPhysicsFrequency(int computerId) {
        State state = STATES.get(computerId);
        if (!ACCELERATED_COMPUTERS.contains(computerId) || state == null) return 0.0;
        double timeStep = state.physicsTimeStepSeconds;
        return Double.isFinite(timeStep) && timeStep > 0.0 ? 1.0 / timeStep : 0.0;
    }

    /** Returns true exactly once per Minecraft game tick for this computer. */
    public static synchronized boolean beginPhysicsTick(int computerId, long gameTick) {
        if (!ACCELERATED_COMPUTERS.contains(computerId)) return false;

        State state = STATES.computeIfAbsent(computerId, ignored -> new State());
        if (state.lastMinecraftTick == gameTick) return false;
        state.lastMinecraftTick = gameTick;
        return true;
    }

    /**
     * Queue a mod peripheral's main-thread continuation for the next Sable
     * physics substep instead of CC:Tweaked's 20 TPS main-thread queue.
     */
    public static boolean enqueueFastTask(int computerId, Runnable task) {
        State state;
        synchronized (HighFrequencyController.class) {
            if (!isAccelerated(computerId)) return false;
            state = STATES.computeIfAbsent(computerId, ignored -> new State());
        }

        int count = state.pendingFastTaskCount.incrementAndGet();
        if (count > MAX_PENDING_FAST_TASKS) {
            state.pendingFastTaskCount.decrementAndGet();
            return false;
        }

        state.fastTasks.add(task);
        if (state.queuedForService.compareAndSet(false, true)) {
            READY_COMPUTERS.add(state);
        }
        return true;
    }

    /**
     * Runs queued fast peripheral work fairly across all computers. The time
     * budget is global per physics substep, so adding computers cannot multiply
     * the maximum main-thread cost.
     */
    public static void drainFastTasks() {
        if (READY_COMPUTERS.isEmpty()) return;
        long deadline = System.nanoTime() + MAX_TASK_TIME_PER_SUBSTEP_NANOS;
        for (int completed = 0; completed < MAX_TASKS_PER_SUBSTEP; completed++) {
            State state = READY_COMPUTERS.poll();
            if (state == null) return;
            if (state.closed) {
                state.fastTasks.clear();
                continue;
            }

            try {
                Runnable task = state.fastTasks.poll();
                if (task != null) {
                    state.pendingFastTaskCount.decrementAndGet();
                    FastPeripheralCallContext.runOptimizedTask(task);
                }
            } finally {
                // A failed task must not strand every later call from this computer.
                state.queuedForService.set(false);
                if (!state.closed && !state.fastTasks.isEmpty()
                    && state.queuedForService.compareAndSet(false, true)) {
                    READY_COMPUTERS.add(state);
                }
            }

            if (System.nanoTime() >= deadline) return;
        }
    }

    public static synchronized void removeComputer(int computerId) {
        State state = STATES.remove(computerId);
        if (state != null) {
            state.closed = true;
            state.fastTasks.clear();
        }
        ACCELERATED_COMPUTERS.remove(computerId);
    }

    private static final class State {
        private volatile boolean closed;
        private double physicsTimeStepSeconds;
        private long lastMinecraftTick = Long.MIN_VALUE;
        private final ConcurrentLinkedQueue<Runnable> fastTasks = new ConcurrentLinkedQueue<>();
        private final AtomicInteger pendingFastTaskCount = new AtomicInteger();
        private final AtomicBoolean queuedForService = new AtomicBoolean();
    }
}
