package com.rustyrelic.hytale.hoardkeeper;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.Config;
import com.rustyrelic.hytale.hoardkeeper.commands.ExcludeCommand;
import com.rustyrelic.hytale.hoardkeeper.commands.HoardkeeperCommand;
import com.rustyrelic.hytale.hoardkeeper.commands.NearCommand;
import com.rustyrelic.hytale.hoardkeeper.commands.StackCommand;
import com.rustyrelic.hytale.hoardkeeper.interaction.HoardstoneInteraction;

import javax.annotation.Nonnull;
import java.nio.file.Files;

/**
 * Wires only: registers the HoardkeeperExcluded marker component and the /hoardkeeper command family.
 * All the actual rules live in StackEngine and HoardkeeperExcluded, not here.
 */
public class HoardkeeperPlugin extends JavaPlugin {

    // Named overload of withConfig -> mods/RustyRelic_hoardkeeper/hoardkeeper.json, not the generic
    // "config.json" the no-name overload would give.
    private static final String CONFIG_NAME = "hoardkeeper";

    /**
     * HoardkeeperExcluded's registered ComponentType. A plain mutable static, NOT a static final field
     * initializer -- this type does not exist at all until setup() calls registerComponent, so there is
     * nothing to lazily defer (contrast StackEngine's InventorySections holder, which defers a call to
     * an engine accessor that already exists but might not be ready yet -- a different problem).
     */
    private static ComponentType<ChunkStore, HoardkeeperExcluded> excludedComponentType;

    @Nonnull
    public static ComponentType<ChunkStore, HoardkeeperExcluded> getExcludedComponentType() {
        return excludedComponentType;
    }

    /**
     * Same plain-static-field pattern as excludedComponentType above. HoardstoneInteraction is built
     * by the codec with no constructor arguments, so there's no way to hand it the settings directly
     * -- a static getter is the only route that reaches it. Safe from setup() onward: nothing that
     * could call getSettings() (a command or the interaction) is even registered yet before setup()
     * runs, and setup() itself only starts once every plugin's config has finished loading -- the
     * engine joins every registered Config's load() future before calling any plugin's setup().
     */
    private static HoardkeeperSettings settings;

    @Nonnull
    public static HoardkeeperSettings getSettings() {
        return settings;
    }

    private final Config<HoardkeeperConfig> config;

    public HoardkeeperPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        config = this.withConfig(CONFIG_NAME, HoardkeeperConfig.CODEC);
    }

    @Override
    protected void setup() {
        // config.get() never blocks here -- see getSettings()'s comment on why loading is already
        // finished by the time setup() runs.
        settings = HoardkeeperSettings.validate(config.get());

        // Config.load() never writes a file, even when none exists -- it just uses the codec's
        // default in memory. So the file is written here instead, and only the first time: an
        // operator always gets something to edit, but an existing file (maybe hand-edited) is never
        // clobbered.
        if (!Files.exists(getDataDirectory().resolve(CONFIG_NAME + ".json"))) {
            config.save();
        }

        // (Class, String id, BuilderCodec) overload only -- the supplier-only overload registers no
        // codec, so nothing written through it would ever be saved. Id "HoardkeeperExcluded" is frozen.
        excludedComponentType = getChunkStoreRegistry()
                .registerComponent(HoardkeeperExcluded.class, "HoardkeeperExcluded", HoardkeeperExcluded.CODEC);

        // Maps the Hoardstone's block JSON ("Type": "RustyRelic_Hoardkeeper_HoardstoneInteraction") to
        // this class -- same codec-registry mechanism QuickStacker uses for its own block interaction.
        getCodecRegistry(Interaction.CODEC).register(
                "RustyRelic_Hoardkeeper_HoardstoneInteraction", HoardstoneInteraction.class, HoardstoneInteraction.CODEC);

        HoardkeeperCommand hoardkeeperCommand = new HoardkeeperCommand("hoardkeeper", "Hoardkeeper commands");
        hoardkeeperCommand.addSubCommand(new StackCommand("stack", "Quick-stack your inventory into nearby chests"));
        hoardkeeperCommand.addSubCommand(new ExcludeCommand("exclude", "Toggle a chest's auto-stack acceptance"));
        hoardkeeperCommand.addSubCommand(new NearCommand("near", "List containers within range"));
        this.getCommandRegistry().registerCommand(hoardkeeperCommand);

        // Permission nodes rustyrelic.hoardkeeper.stack / .exclude are registered automatically as a
        // side effect of requirePermission(...) in StackCommand's/ExcludeCommand's own constructors --
        // AbstractCommand.completeRegistration() calls PermissionsModule.registerPermission(...) for any
        // command that has a permission once it finishes registering. No separate call needed here.
    }

}
