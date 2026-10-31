package dev.sableccticksync;

import dan200.computercraft.core.computer.Computer;

/**
 * Describes which parts of a CC:Tweaked computer tick may advance.
 *
 * <p>Sable may run several physics substeps during one Minecraft tick. Lua and
 * the computer executor should advance on every one of those substeps, while
 * Minecraft-tick-backed state (redstone inputs/events and CC timers) must only
 * advance once per game tick.</p>
 */
public final class ComputerTickDomain {
    private static final ThreadLocal<Integer> PHYSICS_ONLY_DEPTH =
        ThreadLocal.withInitial(() -> 0);

    private ComputerTickDomain() {
    }

    public static void tick(Computer computer, boolean includeMinecraftTickState) {
        if (includeMinecraftTickState) {
            computer.tick();
            return;
        }

        PHYSICS_ONLY_DEPTH.set(PHYSICS_ONLY_DEPTH.get() + 1);
        try {
            computer.tick();
        } finally {
            int depth = PHYSICS_ONLY_DEPTH.get() - 1;
            if (depth <= 0) {
                PHYSICS_ONLY_DEPTH.remove();
            } else {
                PHYSICS_ONLY_DEPTH.set(depth);
            }
        }
    }

    public static boolean isPhysicsOnlyTick() {
        return PHYSICS_ONLY_DEPTH.get() > 0;
    }
}
