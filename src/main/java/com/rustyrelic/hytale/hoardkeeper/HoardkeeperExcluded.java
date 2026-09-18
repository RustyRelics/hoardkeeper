package com.rustyrelic.hytale.hoardkeeper;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;

/**
 * Marks a chest as excluded from Hoardkeeper's auto-stack (design doc rule 7). Fieldless — presence of
 * the component on the chest's block entity is the whole state, so the codec has zero appended fields
 * and {@link #clone()} just returns a new, equally-empty instance.
 * <p>
 * The component id below ("HoardkeeperExcluded", passed to registerComponent) is what gets written to
 * disk and is frozen forever once anyone has used the mod — see design doc §3.
 */
public class HoardkeeperExcluded implements Component<ChunkStore> {

    public static final BuilderCodec<HoardkeeperExcluded> CODEC =
            BuilderCodec.builder(HoardkeeperExcluded.class, HoardkeeperExcluded::new).build();

    public static ComponentType<ChunkStore, HoardkeeperExcluded> getComponentType() {
        return HoardkeeperPlugin.getExcludedComponentType();
    }

    public HoardkeeperExcluded() {
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        return new HoardkeeperExcluded();
    }

    public static boolean isSet(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Ref<ChunkStore> ref) {
        return chunkStore.getComponent(ref, getComponentType()) != null;
    }

    /**
     * The only method in the project that writes this marker. Adding or removing a component on a
     * block entity that already exists does NOT by itself flag the chunk section for saving — that is
     * a separate step, BlockStateInfo.markNeedsSaving, which this always calls after the put/remove.
     * Skipping it would mean the exclusion looks like it worked until the next server restart, when it
     * silently reverts. Confirmed both in the shared source and in a live restart test — see
     * INVESTIGATION-PERSISTENCE.md and SPIKE-NOTES.md's "Marker persistence" section.
     */
    public static void set(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Ref<ChunkStore> ref, boolean excluded) {
        if (excluded) {
            chunkStore.putComponent(ref, getComponentType(), new HoardkeeperExcluded());
        } else {
            chunkStore.removeComponentIfExists(ref, getComponentType());
        }

        BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (blockStateInfo != null) {
            blockStateInfo.markNeedsSaving(chunkStore);
        }
    }

}
