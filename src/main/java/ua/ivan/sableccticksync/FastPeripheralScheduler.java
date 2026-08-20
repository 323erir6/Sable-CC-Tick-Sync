package ua.ivan.sableccticksync;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class FastPeripheralScheduler {
    private static final int MAX_QUEUED_TASKS_PER_COMPUTER = 512;
    private static final Map<Integer, State> STATES = new ConcurrentHashMap<>();

    private FastPeripheralScheduler() {
    }

    public static boolean enqueue(int computerId, Runnable task) {
        State state = STATES.computeIfAbsent(computerId, ignored -> new State());
        int size = state.size.incrementAndGet();
        if (size > MAX_QUEUED_TASKS_PER_COMPUTER) {
            state.size.decrementAndGet();
            return false;
        }

        state.tasks.offer(task);
        return true;
    }

    /**
     * Run queued supported-peripheral calls on the Minecraft/Sable server thread.
     * This must only be called from a server-thread tick or Sable physics event.
     */
    public static int runPending(int computerId, int maxTasks) {
        if (maxTasks <= 0) return 0;

        State state = STATES.get(computerId);
        if (state == null) return 0;

        int executed = 0;
        while (executed < maxTasks) {
            Runnable task = state.tasks.poll();
            if (task == null) break;

            state.size.decrementAndGet();
            task.run();
            executed++;
        }

        if (state.tasks.isEmpty() && state.size.get() <= 0) {
            STATES.remove(computerId, state);
        }

        return executed;
    }

    public static void clearComputer(int computerId) {
        STATES.remove(computerId);
    }

    private static final class State {
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        private final AtomicInteger size = new AtomicInteger();
    }
}
