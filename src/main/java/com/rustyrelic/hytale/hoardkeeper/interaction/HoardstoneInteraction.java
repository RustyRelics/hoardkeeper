package com.rustyrelic.hytale.hoardkeeper.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rustyrelic.hytale.hoardkeeper.StackEngine;
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

    // Same constant the commands use -- milestone 3 replaces every copy of this with one config value.
    private static final double RADIUS = 14.0;

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

        var request = new StackRequest(store, ref, world, position, RADIUS);
        var report = StackEngine.run(request);

        playerRef.sendMessage(Message.raw(report.toChatMessage()));
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
