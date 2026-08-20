package ua.ivan.sableccticksync;

import java.util.Set;

public final class FastPeripheralCallContext {
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "synaxis_dynamic_motor",
            "compact_flap",
            "Create_RotationSpeedController"
    );

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private FastPeripheralCallContext() {
    }

    public static boolean isSupportedType(String type) {
        return type != null && SUPPORTED_TYPES.contains(type);
    }

    public static void enter() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void exit() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    public static boolean isActive() {
        return DEPTH.get() > 0;
    }
}
