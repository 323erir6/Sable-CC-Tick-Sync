package dev.sableccticksync.mixin;

import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.sableccticksync.InventoryListCache;

import java.util.Map;

@Mixin(value = InventoryMethods.class, remap = false)
public abstract class InventoryMethodsMixin {
    @Inject(
        method = "list(Lnet/neoforged/neoforge/items/IItemHandler;)Ljava/util/Map;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void sableCcTickSync$reuseList(
        IItemHandler inventory,
        CallbackInfoReturnable<Map<Integer, Map<String, ?>>> cir
    ) {
        Map<Integer, Map<String, ?>> cached = InventoryListCache.findInventoryList(inventory);
        if (cached != null) cir.setReturnValue(cached);
    }

    @Inject(
        method = "list(Lnet/neoforged/neoforge/items/IItemHandler;)Ljava/util/Map;",
        at = @At("RETURN"),
        remap = false
    )
    private void sableCcTickSync$rememberList(
        IItemHandler inventory,
        CallbackInfoReturnable<Map<Integer, Map<String, ?>>> cir
    ) {
        InventoryListCache.storeInventoryList(inventory, cir.getReturnValue());
    }
}
