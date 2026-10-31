package dev.sableccticksync;

/** Marks a CC peripheral call whose main-thread continuation may be serviced on a physics substep. */
public final class FastPeripheralCallContext {
    private static final ThreadLocal<Integer> FAST_CALL_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> OPTIMIZED_TASK_DEPTH = ThreadLocal.withInitial(() -> 0);

    private FastPeripheralCallContext() {
    }

    /**
     * All real IPeripheral implementations are supported. The old implementation used a short
     * type-name whitelist, which left most peripherals in a modpack locked to the 20 TPS queue.
     */
    public static boolean isSupportedPeripheral(Object target) {
        return target instanceof dan200.computercraft.api.peripheral.IPeripheral;
    }

    /**
     * Mod peripherals are physics-fast by default. Only peripherals whose
     * physical backing is explicitly locked to Minecraft's redstone tick stay
     * on CC:Tweaked's ordinary 20 TPS main-thread scheduler.
     */
    public static boolean isPhysicsFastPeripheral(Object target) {
        if (!isSupportedPeripheral(target)) return false;

        String className = target.getClass().getName();
        // Built-in CC peripherals ultimately observe Minecraft block/entity
        // state. Their direct Lua methods remain fast, but any main-thread
        // continuation must stay on CC's vanilla 20 TPS scheduler.
        if (className.startsWith("dan200.computercraft.shared.peripheral.")) return false;

        // Tom's GPU and its monitor live in Minecraft block entities. GPU draw
        // calls, monitor attachment state and input packets must share the normal
        // server tick. Servicing their main-thread continuations from Sable's
        // pre-physics event can race the monitor's click/event path and leaves a
        // rendered but completely non-interactive screen.
        if (className.startsWith("com.tom.peripherals.")) return false;

        return true;
    }

    public static void enter(boolean physicsFast) {
        if (physicsFast) FAST_CALL_DEPTH.set(FAST_CALL_DEPTH.get() + 1);
    }

    public static void exit(boolean physicsFast) {
        if (!physicsFast) return;

        int depth = FAST_CALL_DEPTH.get() - 1;
        if (depth <= 0) {
            FAST_CALL_DEPTH.remove();
        } else {
            FAST_CALL_DEPTH.set(depth);
        }
    }

    public static boolean isPhysicsFastCall() {
        return FAST_CALL_DEPTH.get() > 0;
    }

    /**
     * Run a main-thread peripheral continuation with the lightweight caches enabled.
     * The continuation is still executed by CC:Tweaked's own fair, time-budgeted
     * scheduler: this marker only enables safe memoisation inside old CC methods.
     */
    public static void runOptimizedTask(Runnable task) {
        OPTIMIZED_TASK_DEPTH.set(OPTIMIZED_TASK_DEPTH.get() + 1);
        try {
            task.run();
        } finally {
            int depth = OPTIMIZED_TASK_DEPTH.get() - 1;
            if (depth <= 0) {
                OPTIMIZED_TASK_DEPTH.remove();
            } else {
                OPTIMIZED_TASK_DEPTH.set(depth);
            }
        }
    }

    public static boolean isOptimizedTask() {
        return OPTIMIZED_TASK_DEPTH.get() > 0;
    }
}
