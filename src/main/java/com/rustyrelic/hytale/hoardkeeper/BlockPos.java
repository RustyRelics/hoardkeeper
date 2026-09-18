package com.rustyrelic.hytale.hoardkeeper;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A plain (x, y, z) position, for display and messages only — it is not used as a key anywhere in this
 * project (there is no longer a set of positions to key; the exclusion marker lives directly on the
 * chest's own block entity).
 */
public record BlockPos(int x, int y, int z) {

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    /**
     * Reads the world position of a block entity, or null if it can't be resolved. Uses
     * BlockStateInfo.fillWorldPos into a throwaway Vector3i and copies the three ints out immediately —
     * that Vector3i is not kept, because Vector3i is mutable and must never be stored directly.
     */
    @Nullable
    public static BlockPos resolve(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Ref<ChunkStore> ref) {
        BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (blockStateInfo == null) return null;

        Vector3i pos = new Vector3i();
        if (!blockStateInfo.fillWorldPos(chunkStore, pos)) return null;

        return new BlockPos(pos.x, pos.y, pos.z);
    }

}
