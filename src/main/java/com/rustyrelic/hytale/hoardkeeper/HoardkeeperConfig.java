package com.rustyrelic.hytale.hoardkeeper;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * The config FILE shape only. A plain mutable class, not a record -- withConfig needs a
 * BuilderCodec, and BuilderCodec needs a mutating Supplier<T> to build into. Nothing else in the
 * mod reads this directly; HoardkeeperSettings is the validated, immutable copy the rest of the
 * mod actually uses.
 * <p>
 * Keys are PascalCase to match every other builtin config codec (e.g. CollisionModuleConfig's
 * "ExtentMax", the template's own "EnableWelcomeMessage").
 */
public class HoardkeeperConfig {

    public static final BuilderCodec<HoardkeeperConfig> CODEC = BuilderCodec.builder(HoardkeeperConfig.class, HoardkeeperConfig::new)
            .append(
                    new KeyedCodec<>("DefaultRadius", Codec.DOUBLE),
                    (config, value) -> config.defaultRadius = value,
                    config -> config.defaultRadius
            )
            .add()
            .append(
                    new KeyedCodec<>("MaxRadius", Codec.DOUBLE),
                    (config, value) -> config.maxRadius = value,
                    config -> config.maxRadius
            )
            .add()
            .append(
                    new KeyedCodec<>("Particles", Codec.BOOLEAN),
                    (config, value) -> config.particles = value,
                    config -> config.particles
            )
            .add()
            .append(
                    new KeyedCodec<>("Sound", Codec.BOOLEAN),
                    (config, value) -> config.sound = value,
                    config -> config.sound
            )
            .add()
            .build();

    // A field missing from the file on disk simply never gets a setter call (BuilderCodec.decodeJson0
    // only visits keys actually present), so whatever this constructor sets stays as the default.
    private double defaultRadius = 14;
    private double maxRadius = 32;
    private boolean particles = true;
    private boolean sound = true;

    public double getDefaultRadius() {
        return defaultRadius;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public boolean isParticles() {
        return particles;
    }

    public boolean isSound() {
        return sound;
    }

}
