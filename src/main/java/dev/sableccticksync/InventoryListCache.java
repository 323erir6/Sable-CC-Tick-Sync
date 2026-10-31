package dev.sableccticksync;

import net.minecraft.core.component.DataComponentPatch;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Very short-lived caches for CC:Tweaked's legacy inventory list operation.
 *
 * <p>InventoryMethods.list serialises every item's data components and hashes the
 * resulting NBT every time it is called. At physics-step frequency that work can dominate the
 * Minecraft server thread even when the inventory has not changed. Component
 * patches are immutable, so their hashes may be safely reused. Complete list
 * results are retained for less than one normal server tick in order to merge
 * duplicate calls without making item counts visibly stale.</p>
 */
public final class InventoryListCache {
    private static final long LIST_CACHE_NANOS = 45_000_000L;
    private static final int MAX_COMPONENT_ENTRIES = 8192;
    private static final int MAX_INVENTORY_ENTRIES = 1024;

    private static final Map<DataComponentPatch, String> COMPONENT_HASHES = new WeakHashMap<>();
    private static final Map<IItemHandler, ListEntry> INVENTORY_LISTS = new WeakHashMap<>();

    private InventoryListCache() {
    }

    public static synchronized Lookup<String> findComponentHash(DataComponentPatch patch) {
        if (!FastPeripheralCallContext.isOptimizedTask()) return Lookup.miss();
        if (!COMPONENT_HASHES.containsKey(patch)) return Lookup.miss();
        return Lookup.hit(COMPONENT_HASHES.get(patch));
    }

    public static synchronized void storeComponentHash(DataComponentPatch patch, String hash) {
        if (!FastPeripheralCallContext.isOptimizedTask()) return;
        if (COMPONENT_HASHES.size() >= MAX_COMPONENT_ENTRIES) COMPONENT_HASHES.clear();
        COMPONENT_HASHES.put(patch, hash);
    }

    public static synchronized Map<Integer, Map<String, ?>> findInventoryList(IItemHandler handler) {
        if (!FastPeripheralCallContext.isOptimizedTask()) return null;

        ListEntry entry = INVENTORY_LISTS.get(handler);
        if (entry == null) return null;
        if (System.nanoTime() - entry.createdAtNanos > LIST_CACHE_NANOS) {
            INVENTORY_LISTS.remove(handler);
            return null;
        }
        return entry.value;
    }

    public static synchronized void storeInventoryList(
        IItemHandler handler,
        Map<Integer, Map<String, ?>> value
    ) {
        if (!FastPeripheralCallContext.isOptimizedTask()) return;
        if (INVENTORY_LISTS.size() >= MAX_INVENTORY_ENTRIES) INVENTORY_LISTS.clear();
        INVENTORY_LISTS.put(handler, new ListEntry(System.nanoTime(), value));
    }

    private record ListEntry(long createdAtNanos, Map<Integer, Map<String, ?>> value) {
    }

    public record Lookup<T>(boolean found, T value) {
        private static <T> Lookup<T> hit(T value) {
            return new Lookup<>(true, value);
        }

        private static <T> Lookup<T> miss() {
            return new Lookup<>(false, null);
        }
    }
}
