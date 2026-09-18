package com.rustyrelic.hytale.hoardkeeper;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3dc;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds chest candidates near a position, closest first, using the engine's own spatial index instead
 * of scanning blocks by hand.
 * <p>
 * Deliberately does not know about HoardkeeperExcluded — every caller (StackEngine, NearCommand,
 * ExcludeCommand) needs a different answer for what to do with an excluded chest (skip it, tag it, or
 * only show it), so that one-line check is left to each caller, exactly like the design doc's own
 * verified call path shows it.
 */
public final class CandidateFinder {

    private CandidateFinder() {
    }

    @Nonnull
    public static List<Ref<ChunkStore>> find(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Vector3dc position, double radius) {
        var spatialResource = chunkStore.getResource(BlockModule.get().getItemContainerSpatialResourceType());

        // This list is reused by every spatial query on this thread and is cleared for us by
        // getThreadLocalReferenceList() itself -- copy the results we want into our own list below and
        // never hand this one back to a caller.
        List<Ref<ChunkStore>> results = SpatialResource.<ChunkStore>getThreadLocalReferenceList();
        spatialResource.getSpatialStructure().ordered(position, radius, results);

        List<Ref<ChunkStore>> candidates = new ArrayList<>();
        for (Ref<ChunkStore> candidateRef : results) {
            if (!candidateRef.isValid()) continue;
            if (chunkStore.getComponent(candidateRef, ItemContainerBlock.getComponentType()) == null) continue;
            candidates.add(candidateRef);
        }
        return candidates;
    }

}
