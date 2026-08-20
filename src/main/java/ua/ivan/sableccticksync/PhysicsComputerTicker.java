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

            HighFrequencyController.setOnConstruction(computer.getID(), true);
            ServerComputerBridge bridge = (ServerComputerBridge) computer;

            // Existing behaviour: one CC tick for every Sable physics substep.
            bridge.sableCcTickSync$physicsTick();

            // Optional mode: add exactly 100 CC ticks per simulated second on top of
            // the Sable-synchronised base rate.
            int extraTicks = HighFrequencyController.consumeExtraTicks(
                    computer.getID(), event.getTimeStep()
            );
            for (int i = 0; i < extraTicks; i++) {
                bridge.sableCcTickSync$physicsTick();
            }
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
