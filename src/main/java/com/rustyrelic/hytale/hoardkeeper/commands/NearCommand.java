package com.rustyrelic.hytale.hoardkeeper.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rustyrelic.hytale.hoardkeeper.BlockPos;
import com.rustyrelic.hytale.hoardkeeper.CandidateFinder;
import com.rustyrelic.hytale.hoardkeeper.HoardkeeperExcluded;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * /hoardkeeper near [--radius=N] -- read-only diagnostic. Lists every chest CandidateFinder returns,
 * with its position, slot usage, and an EXCLUDED tag for chests /hoardkeeper stack would skip. Doesn't
 * move or change anything.
 */
public class NearCommand extends AbstractPlayerCommand {

    private static final double DEFAULT_RADIUS = 14.0;
    private static final int MAX_ITEM_IDS_SHOWN = 3;

    @Nonnull
    private final DefaultArg<Double> radiusArg = withDefaultArg(
            "radius", "Search radius in blocks", ArgTypes.DOUBLE, DEFAULT_RADIUS, "14"
    );

    public NearCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        requireNoPermission(); // diagnostic only -- design doc says this command needs no node
    }

    @Override
    protected void execute(
            @Nonnull final CommandContext context,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final PlayerRef playerRef,
            @Nonnull final World world) {

        double radius = radiusArg.get(context);

        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            context.sendMessage(Message.raw("near: could not determine your position."));
            return;
        }

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        List<Ref<ChunkStore>> candidates = CandidateFinder.find(chunkStore, transform.getPosition(), radius);

        StringBuilder out = new StringBuilder("near: radius=").append(radius)
                .append(" found=").append(candidates.size()).append('\n');

        int shown = 0;
        for (Ref<ChunkStore> candidateRef : candidates) {
            shown++;

            BlockPos pos = BlockPos.resolve(chunkStore, candidateRef);
            boolean excluded = HoardkeeperExcluded.isSet(chunkStore, candidateRef);

            ItemContainerBlock containerBlock = chunkStore.getComponent(candidateRef, ItemContainerBlock.getComponentType());
            ItemContainer container = containerBlock.getItemContainer();
            short capacity = container.getCapacity();

            int used = 0;
            StringBuilder ids = new StringBuilder();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (ItemStack.isEmpty(stack)) continue;
                used++;
                if (used <= MAX_ITEM_IDS_SHOWN) {
                    if (ids.length() > 0) ids.append(", ");
                    ids.append(stack.getItemId()).append('x').append(stack.getQuantity());
                }
            }
            if (used > MAX_ITEM_IDS_SHOWN) ids.append(", ...");

            out.append(shown).append(". ").append(pos == null ? "(unresolved position)" : pos.toString())
                    .append("  slots=").append(used).append('/').append(capacity)
                    .append("  ").append(used == 0 ? "(empty)" : ids)
                    .append(excluded ? "  [EXCLUDED]" : "")
                    .append('\n');
        }

        context.sendMessage(Message.raw(out.toString().stripTrailing()));
    }

}
