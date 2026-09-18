package com.rustyrelic.hytale.hoardkeeper.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rustyrelic.hytale.hoardkeeper.StackEngine;
import com.rustyrelic.hytale.hoardkeeper.StackReport;
import com.rustyrelic.hytale.hoardkeeper.StackRequest;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * /hoardkeeper stack [--radius=N] -- runs StackEngine at the player's own position and prints the
 * report. All the "what moved and why" logic lives in StackEngine; this class only reads a flag,
 * builds a request, and formats the result.
 */
public class StackCommand extends AbstractPlayerCommand {

    private static final double DEFAULT_RADIUS = 14.0;

    @Nonnull
    private final DefaultArg<Double> radiusArg = withDefaultArg(
            "radius", "Search radius in blocks", ArgTypes.DOUBLE, DEFAULT_RADIUS, "14"
    );

    public StackCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        requirePermission("rustyrelic.hoardkeeper.stack");
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
            context.sendMessage(Message.raw("stack: could not determine your position."));
            return;
        }

        StackRequest request = new StackRequest(store, ref, world, transform.getPosition(), radius);
        StackReport report = StackEngine.run(request);

        StringBuilder out = new StringBuilder();
        out.append("Hoardkeeper: moved ").append(report.getTotalMoved()).append(" item(s)");
        if (report.getSkippedExcludedCount() > 0) {
            out.append(" (skipped ").append(report.getSkippedExcludedCount()).append(" excluded chest(s))");
        }
        out.append('\n');

        for (StackReport.ChestResult chestResult : report.getChestResults()) {
            out.append("  ").append(chestResult.position()).append(": ")
                    .append(chestResult.movedCount()).append(" moved\n");
        }

        for (Map.Entry<String, Integer> entry : report.getChestsFull().entrySet()) {
            out.append("  ").append(entry.getKey()).append(" x").append(entry.getValue())
                    .append(" stayed in inventory -- chests holding it are full\n");
        }

        for (Map.Entry<String, Integer> entry : report.getNoEligibleChest().entrySet()) {
            out.append("  ").append(entry.getKey()).append(" x").append(entry.getValue())
                    .append(" stayed in inventory -- nothing in range holds it\n");
        }

        context.sendMessage(Message.raw(out.toString().stripTrailing()));
    }

}
