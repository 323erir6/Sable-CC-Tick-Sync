package ua.ivan.sableccticksync;

import dan200.computercraft.api.ComputerCraftAPI;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SableCcTickSync.MOD_ID)
public final class SableCcTickSync {
    public static final String MOD_ID = "sable_cc_tick_sync";

    public SableCcTickSync() {
        // Always create the lightweight API wrapper. It does not touch world/Sable
        // state from CC's constructor or computer thread. Whether the global name
        // is exposed is decided later from a server-thread cache.
        ComputerCraftAPI.registerAPIFactory(SableSyncApi::new);

        NeoForge.EVENT_BUS.addListener(
            EventPriority.HIGHEST,
            PhysicsComputerTicker::onPrePhysicsTick
        );
    }
}
