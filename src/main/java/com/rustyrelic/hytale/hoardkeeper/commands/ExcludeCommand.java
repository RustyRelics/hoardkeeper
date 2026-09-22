package com.rustyrelic.hytale.hoardkeeper.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rustyrelic.hytale.hoardkeeper.BlockPos;
import com.rustyrelic.hytale.hoardkeeper.CandidateFinder;
import com.rustyrelic.hytale.hoardkeeper.HoardkeeperExcluded;
import com.rustyrelic.hytale.hoardkeeper.HoardkeeperPlugin;
import com.rustyrelic.hytale.hoardkeeper.RadiusResolver;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * /hoardkeeper exclude [--list | --clear] [--radius=N] -- toggles the one chest the player has open;
 * --list and --clear both work on excluded chests within range, since there is no separate stored
 * list to read: the marker lives on each chest's own block entity.
 */
public class ExcludeCommand extends AbstractPlayerCommand {

    @Nonnull
    private final FlagArg listArg = withFlagArg("list", "List excluded chests within range");
    @Nonnull
    private final FlagArg clearArg = withFlagArg("clear", "Un-exclude every excluded chest within range");
    @Nonnull
    private final OptionalArg<Double> radiusArg = withOptionalArg(
            "radius", "Search radius in blocks, for --list/--clear", ArgTypes.DOUBLE
    );

    public ExcludeCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        requirePermission("rustyrelic.hoardkeeper.exclude");
    }

    @Override
    protected void execute(
            @Nonnull final CommandContext context,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final PlayerRef playerRef,
            @Nonnull final World world) {

        // --clear and --list don't touch the open window at all, so they're handled first and return.
        if (clearArg.get(context)) {
            clearNearby(context, store, ref, world);
            return;
        }
        if (listArg.get(context)) {
            listNearby(context, store, ref, world);
            return;
        }
        toggleOpenChest(context, store, ref, world);
    }

    private void toggleOpenChest(
            @Nonnull final CommandContext context,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final World world) {

        var playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            context.sendMessage(Message.raw("exclude: could not resolve your Player component."));
            return;
        }

        List<ContainerBlockWindow> openContainers = new ArrayList<>();
        for (Window window : playerComponent.getWindowManager().getWindows()) {
            if (window instanceof ContainerBlockWindow containerWindow) {
                openContainers.add(containerWindow);
            }
        }

        if (openContainers.isEmpty()) {
            context.sendMessage(Message.raw("exclude: open a chest first."));
            return;
        }

        if (openContainers.size() > 1) {
            // Structurally possible -- nothing closes a previously-open container window before a new
            // one opens. Report both rather than guessing which one the player means.
            StringBuilder out = new StringBuilder("exclude: you have ").append(openContainers.size())
                    .append(" chests open at once -- not guessing which one you mean:\n");
            for (ContainerBlockWindow containerWindow : openContainers) {
                out.append("  (").append(containerWindow.getX()).append(", ").append(containerWindow.getY())
                        .append(", ").append(containerWindow.getZ()).append(")\n");
            }
            context.sendMessage(Message.raw(out.toString().stripTrailing()));
            return;
        }

        ContainerBlockWindow window = openContainers.get(0);
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, window.getX(), window.getY(), window.getZ());
        if (blockRef == null) {
            // Should not happen for a chest the player has open right now -- the chunk it's in must be
            // loaded. Reporting rather than falling back to the spatial query, per the design doc.
            context.sendMessage(Message.raw("exclude: could not resolve a block entity at ("
                    + window.getX() + ", " + window.getY() + ", " + window.getZ() + ") for the open chest."));
            return;
        }

        ItemContainerBlock containerBlock = chunkStore.getComponent(blockRef, ItemContainerBlock.getComponentType());
        if (containerBlock == null) {
            context.sendMessage(Message.raw("exclude: the open window isn't attached to an item container."));
            return;
        }

        BlockPos pos = new BlockPos(window.getX(), window.getY(), window.getZ());
        boolean nowExcluded = !HoardkeeperExcluded.isSet(chunkStore, blockRef);
        HoardkeeperExcluded.set(chunkStore, blockRef, nowExcluded);

        context.sendMessage(Message.raw("exclude: chest at " + pos + " is now "
                + (nowExcluded ? "excluded" : "included") + "."));
    }

    private void listNearby(
            @Nonnull final CommandContext context,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final World world) {

        RadiusResolver.Result radiusResult = resolveRadius(context);
        double radius = radiusResult.radius();
        List<BlockPos> excludedNearby = findExcludedNearby(store, ref, world, radius);

        StringBuilder out = new StringBuilder();
        if (radiusResult.clamped()) {
            out.append("radius capped at ").append((int) radius).append('\n');
        }

        if (excludedNearby.isEmpty()) {
            out.append("exclude: no excluded chests within range (radius=").append((int) radius)
                    .append("). This checks chests within range, not a global list -- the marker lives in chunk data.");
            context.sendMessage(Message.raw(out.toString()));
            return;
        }

        out.append("exclude: ").append(excludedNearby.size())
                .append(" excluded chest(s) within range (radius=").append((int) radius).append("):\n");
        for (BlockPos pos : excludedNearby) {
            out.append("  ").append(pos).append('\n');
        }
        context.sendMessage(Message.raw(out.toString().stripTrailing()));
    }

    private void clearNearby(
            @Nonnull final CommandContext context,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final World world) {

        RadiusResolver.Result radiusResult = resolveRadius(context);
        double radius = radiusResult.radius();

        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            context.sendMessage(Message.raw("exclude: could not determine your position."));
            return;
        }

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        List<Ref<ChunkStore>> candidates = CandidateFinder.find(chunkStore, transform.getPosition(), radius);

        int cleared = 0;
        for (Ref<ChunkStore> candidateRef : candidates) {
            if (!HoardkeeperExcluded.isSet(chunkStore, candidateRef)) continue;
            HoardkeeperExcluded.set(chunkStore, candidateRef, false);
            cleared++;
        }

        StringBuilder out = new StringBuilder();
        if (radiusResult.clamped()) {
            out.append("radius capped at ").append((int) radius).append('\n');
        }
        out.append("exclude: un-excluded ").append(cleared).append(" chest(s) within range (radius=").append((int) radius).append(").");
        context.sendMessage(Message.raw(out.toString()));
    }

    @Nonnull
    private List<BlockPos> findExcludedNearby(
            @Nonnull final Store<EntityStore> store,
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final World world,
            double radius) {

        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return List.of();

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        List<Ref<ChunkStore>> candidates = CandidateFinder.find(chunkStore, transform.getPosition(), radius);

        List<BlockPos> excludedNearby = new ArrayList<>();
        for (Ref<ChunkStore> candidateRef : candidates) {
            if (!HoardkeeperExcluded.isSet(chunkStore, candidateRef)) continue;
            BlockPos pos = BlockPos.resolve(chunkStore, candidateRef);
            if (pos != null) excludedNearby.add(pos);
        }
        return excludedNearby;
    }

    @Nonnull
    private RadiusResolver.Result resolveRadius(@Nonnull CommandContext context) {
        Double requestedRadius = radiusArg.provided(context) ? radiusArg.get(context) : null;
        return RadiusResolver.resolve(requestedRadius, HoardkeeperPlugin.getSettings());
    }

}
