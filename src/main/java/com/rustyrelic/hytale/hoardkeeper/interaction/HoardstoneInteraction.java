package com.rustyrelic.hytale.hoardkeeper.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rustyrelic.hytale.hoardkeeper.HoardkeeperPlugin;
import com.rustyrelic.hytale.hoardkeeper.HoardkeeperSettings;
import com.rustyrelic.hytale.hoardkeeper.StackEngine;
import com.rustyrelic.hytale.hoardkeeper.StackReport;
import com.rustyrelic.hytale.hoardkeeper.StackRequest;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Use on the Hoardstone -- same StackEngine as /hk stack, but the search origin is the block's own
 * position, not the player's. No permission check: placing the block is the authorisation.
 */
public class HoardstoneInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<HoardstoneInteraction> CODEC =
            BuilderCodec.builder(HoardstoneInteraction.class, HoardstoneInteraction::new).build();

    @Override
    protected void interactWithBlock(
            @Nonnull final World world,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer,
            @Nonnull final InteractionType type,
            @Nonnull final InteractionContext context,
            @Nullable final ItemStack itemInHand,
            @Nonnull final Vector3i targetBlock,
            @Nonnull final CooldownHandler cooldownHandler) {

        var ref = context.getEntity();
        var store = ref.getStore();
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        // StackRequest wants a Vector3dc; targetBlock is the block's integer corner, so this is the
        // adapter -- +0.5 on each axis puts the search origin at the block's center, not its corner.
        var position = new Vector3d(targetBlock.x + 0.5, targetBlock.y + 0.5, targetBlock.z + 0.5);

        HoardkeeperSettings settings = HoardkeeperPlugin.getSettings();

        // The Hoardstone has no --radius flag, so it always uses the configured default -- same rule
        // RadiusResolver applies everywhere else, just never the "given" branch.
        var request = new StackRequest(store, ref, world, position, settings.defaultRadius());
        var report = StackEngine.run(request);

        playerRef.sendMessage(Message.raw(report.toChatMessage()));

        playEffects(report, position, store, settings);
    }

    /**
     * SUCCESS (moved at least one item, even partially) plays the burst particle and heal-style
     * sound; NOPE (moved nothing -- no eligible chest, all full, or nothing loose to move) plays
     * only the sleep-fail sound. Both sound calls resolve their asset id to an int index right here
     * at play time via a map lookup, not cached in a static field -- the asset map may not be ready
     * yet when this class is first loaded, the same trap component types have.
     */
    private static void playEffects(
            @Nonnull StackReport report,
            @Nonnull Vector3d position,
            @Nonnull Store<EntityStore> store,
            @Nonnull HoardkeeperSettings settings) {

        boolean success = report.getTotalMoved() >= 1;

        if (success) {
            if (settings.particles()) {
                // Particles take the asset id String directly -- no index lookup, unlike sounds.
                ParticleUtil.spawnParticleEffect(HoardstoneEffects.SUCCESS_PARTICLE, position, store);
            }
            if (settings.sound()) {
                playSound(HoardstoneEffects.SUCCESS_SOUND, position, store);
            }
        } else if (settings.sound()) {
            playSound(HoardstoneEffects.NOPE_SOUND, position, store);
        }
    }

    private static void playSound(
            @Nonnull String soundEventId,
            @Nonnull Vector3d position,
            @Nonnull Store<EntityStore> store) {
        int soundIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
        SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, position.x, position.y, position.z, store);
    }

    @Override
    protected void simulateInteractWithBlock(
            @Nonnull final InteractionType type,
            @Nonnull final InteractionContext context,
            @Nullable final ItemStack itemInHand,
            @Nonnull final World world,
            @Nonnull final Vector3i targetBlock) {
        // No client-side prediction needed -- StackEngine only runs on the server.
    }

}
