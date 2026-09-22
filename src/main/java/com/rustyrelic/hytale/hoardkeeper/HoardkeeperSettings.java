package com.rustyrelic.hytale.hoardkeeper;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;

/**
 * The validated, immutable settings the rest of the mod actually reads -- built once from the
 * loaded HoardkeeperConfig. Nothing writes this after it's built, so there's still no
 * plugin-level mutable state; a future reload would just build a new one and swap the reference
 * that holds it.
 */
public record HoardkeeperSettings(double defaultRadius, double maxRadius, boolean particles, boolean sound) {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double DEFAULT_RADIUS_FALLBACK = 14;
    private static final double MAX_RADIUS_FALLBACK = 32;

    /**
     * Builds settings from a loaded config, correcting bad values instead of failing. Each
     * correction logs one WARN naming the field, the bad value, and the value used instead.
     */
    @Nonnull
    public static HoardkeeperSettings validate(@Nonnull HoardkeeperConfig config) {
        double maxRadius = config.getMaxRadius();
        if (maxRadius < 1) {
            LOGGER.atWarning().log("Hoardkeeper config: MaxRadius %s is less than 1, using %s instead.", maxRadius, MAX_RADIUS_FALLBACK);
            maxRadius = MAX_RADIUS_FALLBACK;
        }

        double defaultRadius = config.getDefaultRadius();
        if (defaultRadius < 1) {
            LOGGER.atWarning().log("Hoardkeeper config: DefaultRadius %s is less than 1, using %s instead.", defaultRadius, DEFAULT_RADIUS_FALLBACK);
            defaultRadius = DEFAULT_RADIUS_FALLBACK;
        } else if (defaultRadius > maxRadius) {
            LOGGER.atWarning().log("Hoardkeeper config: DefaultRadius %s is greater than MaxRadius %s, clamping to %s.", defaultRadius, maxRadius, maxRadius);
            defaultRadius = maxRadius;
        }

        return new HoardkeeperSettings(defaultRadius, maxRadius, config.isParticles(), config.isSound());
    }

}
