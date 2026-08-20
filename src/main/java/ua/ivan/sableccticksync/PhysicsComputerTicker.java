package ua.ivan.sableccticksync;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class PhysicsComputerTicker {
    private static final int MAX_FAST_PERIPHERAL_TASKS_PER_PASS = 64;

    private PhysicsComputerTicker() {
    }

    /**
     * This method must only be called from the Minecraft server thread.
     */
    public static boolean usesPhysicsTicks(ServerComputer computer) {
        return findLiveConstruction(computer.getLevel(), computer.getPosition()) != null;
    }

    public static void refreshConstructionState(ServerComputer computer) {
        boolean onConstruction = usesPhysicsTicks(computer);
        HighFrequencyController.setOnConstruction(computer.getID(), onConstruction);
    }

    public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        SubLevelPhysicsSystem physicsSystem = event.getPhysicsSystem();
        var level = physicsSystem.getLevel();
        var server = level.getServer();

        List<ServerComputer> computers = List.copyOf(
            ServerContext.get(server).registry().getComputers()
        );

        for (ServerComputer computer : computers) {
            if (computer.getLevel() != level) continue;

            ServerSubLevel construction = findLiveConstruction(
                    computer.getLevel(), computer.getPosition()
            );
            if (construction == null || construction.getLevel() != level) {
                HighFrequencyController.setOnConstruction(computer.getID(), false);
                continue;
            }

            int computerId = computer.getID();
            HighFrequencyController.setOnConstruction(computerId, true);
            ServerComputerBridge bridge = (ServerComputerBridge) computer;

            // Execute supported main-thread peripheral work immediately at the
            // physics boundary instead of waiting for CC:Tweaked's normal
            // Minecraft-tick MainThread scheduler.
            FastPeripheralScheduler.runPending(
                    computerId,
                    MAX_FAST_PERIPHERAL_TASKS_PER_PASS
            );

            // Existing behaviour: one CC tick for every Sable physics substep.
            bridge.sableCcTickSync$physicsTick();

            // Optional mode: add exactly 100 CC logical ticks per simulated
            // second on top of the Sable-synchronised base rate.
            int extraTicks = HighFrequencyController.consumeExtraTicks(
                    computerId, event.getTimeStep()
            );
            for (int i = 0; i < extraTicks; i++) {
                bridge.sableCcTickSync$physicsTick();
            }

            // The computer thread runs independently. A supported peripheral
            // call may have been queued while the logical ticks above were being
            // processed, so make a second opportunistic pass before Sable
            // advances to the next physics substep.
            FastPeripheralScheduler.runPending(
                    computerId,
                    MAX_FAST_PERIPHERAL_TASKS_PER_PASS
            );
        }
    }

    private static ServerSubLevel findLiveConstruction(ServerLevel level, BlockPos position) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, position);

        if (subLevel instanceof ServerSubLevel serverSubLevel
            && !serverSubLevel.isRemoved()) {
            return serverSubLevel;
        }

        return null;
    }
}
