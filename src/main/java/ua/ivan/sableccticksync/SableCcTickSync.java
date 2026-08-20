package ua.ivan.sableccticksync;

import dan200.computercraft.api.ComputerCraftAPI;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SableCcTickSync.MOD_ID)
public final class SableCcTickSync {
    public static final String MOD_ID = "sable_cc_tick_sync";

    public SableCcTickSync() {
        ComputerCraftAPI.registerAPIFactory(computer ->
            PhysicsComputerTicker.usesPhysicsTicks(computer)
                ? new SableSyncApi(computer)
                : null
        );

        NeoForge.EVENT_BUS.addListener(
            EventPriority.HIGHEST,
            PhysicsComputerTicker::onPrePhysicsTick
        );
    }
}
