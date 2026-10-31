package dev.sableccticksync;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PhysicsComputerTicker {
    private static MinecraftServer cachedServer;
    private static int cachedServerTick = Integer.MIN_VALUE;
    private static final Map<ServerLevel, List<ServerComputer>> COMPUTERS_BY_LEVEL =
        new IdentityHashMap<>();

    private PhysicsComputerTicker() {
    }

    public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        SubLevelPhysicsSystem physicsSystem = event.getPhysicsSystem();
        var level = physicsSystem.getLevel();
        var server = level.getServer();

        refreshComputerIndex(server);
        List<ServerComputer> computers = COMPUTERS_BY_LEVEL.getOrDefault(level, Collections.emptyList());

        // The queue is global, so servicing it once from the overworld physics
        // pass preserves physics-step latency without multiplying its time
        // budget by the number of loaded dimensions.
        if (level == server.overworld()) HighFrequencyController.drainFastTasks();

        for (ServerComputer computer : computers) {
            int computerId = computer.getID();
            HighFrequencyController.recordPhysicsStep(computerId, event.getTimeStep());

            ServerComputerBridge bridge = (ServerComputerBridge) computer;
            // ServerComputer.tickServer now always supplies CC:Tweaked's one
            // authoritative Minecraft tick. Skip one Sable substep in the same
            // game tick so the total number of Computer ticks still follows the
            // Sable physics frequency instead of physicsFrequency + 20 Hz.
            //
            // Keeping the vanilla tick is essential for block-backed peripherals:
            // Tom's monitor touches, redstone and attachment changes are produced
            // in Minecraft's tick domain and must not depend on Sable event order.
            boolean representedByVanillaTick = HighFrequencyController.beginPhysicsTick(
                computerId,
                level.getGameTime()
            );
            if (!representedByVanillaTick) {
                bridge.sableCcTickSync$physicsTick(false);
            }
        }
    }

    private static void refreshComputerIndex(MinecraftServer server) {
        int serverTick = server.getTickCount();
        if (cachedServer == server && cachedServerTick == serverTick) return;

        cachedServer = server;
        cachedServerTick = serverTick;
        COMPUTERS_BY_LEVEL.clear();
        for (ServerComputer computer : ServerContext.get(server).registry().getComputers()) {
            if (!(computer.getLevel() instanceof ServerLevel level)) continue;
            HighFrequencyController.registerComputer(computer.getID());
            COMPUTERS_BY_LEVEL.computeIfAbsent(level, ignored -> new ArrayList<>()).add(computer);
        }
    }

    public static void clear() {
        cachedServer = null;
        cachedServerTick = Integer.MIN_VALUE;
        COMPUTERS_BY_LEVEL.clear();
    }
}
