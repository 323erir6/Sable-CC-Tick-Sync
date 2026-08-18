package ua.ivan.sableccticksync;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

import java.util.List;

public final class PhysicsComputerTicker {
    private PhysicsComputerTicker() {
    }

    public static boolean usesPhysicsTicks(ServerComputer computer) {
        return findLiveConstruction(computer) != null;
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

            ServerSubLevel construction = findLiveConstruction(computer);
            if (construction == null || construction.getLevel() != level) continue;

            ((ServerComputerBridge) computer).sableCcTickSync$physicsTick();
        }
    }

    private static ServerSubLevel findLiveConstruction(ServerComputer computer) {
        SubLevel subLevel = Sable.HELPER.getContaining(
            computer.getLevel(),
            computer.getPosition()
        );

        if (subLevel instanceof ServerSubLevel serverSubLevel
            && !serverSubLevel.isRemoved()) {
            return serverSubLevel;
        }

        return null;
    }
}
