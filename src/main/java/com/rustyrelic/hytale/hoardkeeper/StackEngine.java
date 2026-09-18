package com.rustyrelic.hytale.hoardkeeper;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The whole operation: read the player's Storage+Backpack, find nearby non-excluded chests, quick-stack
 * into each one closest-first, and report what happened. This is the only class in the project that
 * knows the stacking rules (design doc §2) — commands just call it and print what it returns.
 */
public final class StackEngine {

    private StackEngine() {
    }

    @Nonnull
    public static StackReport run(@Nonnull StackRequest request) {
        Store<ChunkStore> chunkStore = request.world().getChunkStore().getStore();

        // {Storage, Backpack} only -- never Hotbar/Armor/Utility/Tool (design doc rules 5 and 6). Uses
        // InventorySections.STORAGE_BACKPACK below rather than a fresh array every call, because
        // InventoryComponent.getCombined caches its result keyed by the exact array instance passed in.
        ItemContainer source = InventoryComponent.getCombined(request.store(), request.sourceRef(), InventorySections.STORAGE_BACKPACK);

        Map<String, Integer> before = summarize(source);

        List<Ref<ChunkStore>> candidates = CandidateFinder.find(chunkStore, request.position(), request.radius());

        List<StackReport.ChestResult> chestResults = new ArrayList<>();
        // Every item id that at least one chest already had a stack of, whether or not it fully fit --
        // used after the loop to tell "nothing in range holds this" apart from "the chest(s) are full".
        Set<String> attemptedItemIds = new HashSet<>();
        int totalMoved = 0;
        int skippedExcluded = 0;

        for (Ref<ChunkStore> candidateRef : candidates) {
            // Rule 7: an excluded chest is never a destination. Checked here, per candidate, rather than
            // inside CandidateFinder -- see CandidateFinder's own class comment.
            if (HoardkeeperExcluded.isSet(chunkStore, candidateRef)) {
                skippedExcluded++;
                continue;
            }

            ItemContainerBlock containerBlock = chunkStore.getComponent(candidateRef, ItemContainerBlock.getComponentType());
            if (containerBlock == null) continue;

            // One quickStackTo call per chest, never the varargs multi-chest form -- design doc §3.
            var listTransaction = source.quickStackTo(containerBlock.getItemContainer());

            int movedHere = 0;
            for (MoveTransaction<ItemStackTransaction> moveTransaction : listTransaction.getList()) {
                if (moveTransaction == null) continue;
                ItemStackTransaction addTransaction = moveTransaction.getAddTransaction();
                if (addTransaction == null) continue;

                ItemStack query = addTransaction.getQuery();
                if (query == null || ItemStack.isEmpty(query)) continue;
                attemptedItemIds.add(query.getItemId());

                ItemStack remainder = addTransaction.getRemainder();
                int remainderQuantity = ItemStack.isEmpty(remainder) ? 0 : remainder.getQuantity();
                int movedQuantity = query.getQuantity() - remainderQuantity;
                if (movedQuantity > 0) movedHere += movedQuantity;
            }

            BlockPos pos = BlockPos.resolve(chunkStore, candidateRef);
            if (pos != null) {
                chestResults.add(new StackReport.ChestResult(pos, movedHere));
            }
            totalMoved += movedHere;
        }

        Map<String, Integer> after = summarize(source);
        Map<String, Integer> noEligibleChest = new LinkedHashMap<>();
        Map<String, Integer> chestsFull = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : after.entrySet()) {
            if (attemptedItemIds.contains(entry.getKey())) {
                chestsFull.put(entry.getKey(), entry.getValue());
            } else {
                noEligibleChest.put(entry.getKey(), entry.getValue());
            }
        }

        return new StackReport(chestResults, totalMoved, noEligibleChest, chestsFull, skippedExcluded);
    }

    @Nonnull
    private static Map<String, Integer> summarize(@Nonnull ItemContainer container) {
        Map<String, Integer> byItem = new LinkedHashMap<>();
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (ItemStack.isEmpty(stack)) continue;
            byItem.merge(stack.getItemId(), stack.getQuantity(), Integer::sum);
        }
        return byItem;
    }

    /**
     * Lazy holder for the {Storage, Backpack} component-type array. Must be a single stable array
     * instance for getCombined's cache to work, and must NOT be a plain static field on this class --
     * that would run at class-load time, which happens during plugin construction, before the engine
     * has finished registering the Storage/Backpack component types, and would permanently bake in
     * null (this crashed the spike the first time it was tried — see SPIKE-NOTES.md). Putting the array
     * in a private nested class defers that first call until a command actually runs StackEngine, by
     * which point the server is fully started.
     */
    private static final class InventorySections {
        @SuppressWarnings("unchecked")
        static final ComponentType<EntityStore, ? extends InventoryComponent>[] STORAGE_BACKPACK =
                new ComponentType[]{
                        InventoryComponent.Storage.getComponentType(),
                        InventoryComponent.Backpack.getComponentType()
                };
    }

}
