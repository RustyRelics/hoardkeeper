package com.rustyrelic.hytale.hoardkeeper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The one place radius rules live, used by every call site that needs a radius: /hk stack,
 * /hk near, /hk exclude --list, /hk exclude --clear, and the Hoardstone (which always passes null,
 * since it has no --radius flag -- same rule, just never the "given" branch).
 */
public final class RadiusResolver {

    private RadiusResolver() {
    }

    public record Result(double radius, boolean clamped) {
    }

    /**
     * @param requested the player's --radius value, or null if they didn't give one
     */
    @Nonnull
    public static Result resolve(@Nullable Double requested, @Nonnull HoardkeeperSettings settings) {
        if (requested == null) {
            return new Result(settings.defaultRadius(), false);
        }

        double clampedRadius = Math.max(1, Math.min(requested, settings.maxRadius()));
        return new Result(clampedRadius, clampedRadius != requested);
    }

}
