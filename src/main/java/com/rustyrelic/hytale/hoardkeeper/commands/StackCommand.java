package com.rustyrelic.hytale.hoardkeeper.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rustyrelic.hytale.hoardkeeper.HoardkeeperPlugin;
import com.rustyrelic.hytale.hoardkeeper.RadiusResolver;
import com.rustyrelic.hytale.hoardkeeper.StackEngine;
import com.rustyrelic.hytale.hoardkeeper.StackReport;
import com.rustyrelic.hytale.hoardkeeper.StackRequest;

import javax.annotation.Nonnull;

/**
 * /hoardkeeper stack [--radius=N] -- runs StackEngine at the player's own position and prints the
 * report. All the "what moved and why" logic lives in StackEngine; this class only reads a flag,
 * builds a request, and formats the result.
 */
public class StackCommand extends AbstractPlayerCommand {

    @Nonnull
    private final OptionalArg<Double> radiusArg = withOptionalArg(
            "radius", "Search radius in blocks", ArgTypes.DOUBLE
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

        // provided() must be checked before get() -- an unprovided OptionalArg's get() returns null
        // with no warning, per CommandContext.get()'s own doc comment.
        Double requestedRadius = radiusArg.provided(context) ? radiusArg.get(context) : null;
        RadiusResolver.Result radiusResult = RadiusResolver.resolve(requestedRadius, HoardkeeperPlugin.getSettings());

        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            context.sendMessage(Message.raw("stack: could not determine your position."));
            return;
        }

        StackRequest request = new StackRequest(store, ref, world, transform.getPosition(), radiusResult.radius());
        StackReport report = StackEngine.run(request);

        String message = report.toChatMessage();
        if (radiusResult.clamped()) {
            message = "radius capped at " + (int) radiusResult.radius() + "\n" + message;
        }
        context.sendMessage(Message.raw(message));
    }

}
