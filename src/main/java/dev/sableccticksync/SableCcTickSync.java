package dev.sableccticksync;

import dan200.computercraft.api.ComputerCraftAPI;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SableCcTickSync.MOD_ID)
public final class SableCcTickSync {
    public static final String MOD_ID = "sable_cc_tick_sync";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SableCcTickSync(IEventBus modBus) {
        ComputerCraftAPI.registerAPIFactory(SableSyncApi::new);
        NeoForge.EVENT_BUS.addListener(
            EventPriority.HIGHEST,
            PhysicsComputerTicker::onPrePhysicsTick
        );
        NeoForge.EVENT_BUS.addListener(SableCcTickSync::onServerStopped);
    }

    private static void onServerStopped(ServerStoppedEvent ignored) {
        PhysicsComputerTicker.clear();
    }
}
