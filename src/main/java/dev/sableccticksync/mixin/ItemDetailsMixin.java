package dev.sableccticksync.mixin;

import dan200.computercraft.shared.details.ItemDetails;
import net.minecraft.core.component.DataComponentPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.sableccticksync.InventoryListCache;

@Mixin(value = ItemDetails.class, remap = false)
public abstract class ItemDetailsMixin {
    @Inject(
        method = "getComponentHash(Lnet/minecraft/core/component/DataComponentPatch;)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void sableCcTickSync$reuseComponentHash(
        DataComponentPatch patch,
        CallbackInfoReturnable<String> cir
    ) {
        InventoryListCache.Lookup<String> cached = InventoryListCache.findComponentHash(patch);
        if (cached.found()) cir.setReturnValue(cached.value());
    }

    @Inject(
        method = "getComponentHash(Lnet/minecraft/core/component/DataComponentPatch;)Ljava/lang/String;",
        at = @At("RETURN"),
        remap = false
    )
    private static void sableCcTickSync$rememberComponentHash(
        DataComponentPatch patch,
        CallbackInfoReturnable<String> cir
    ) {
        InventoryListCache.storeComponentHash(patch, cir.getReturnValue());
    }
}
