package dev.xylonity.knightlib.api.config;

import dev.xylonity.knightlib.client.screen.config.NeoForgeConfigScreens;
import dev.xylonity.knightlib.config.interop.ConfigManager;
import dev.xylonity.knightlib.config.interop.ConfigRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

/**
 * NeoForge entry point for the KnightLib configuration system.
 *
 * <h3>Usage from any mod:</h3>
 * <pre>{@code
 * // In your mod constructor:
 * ConfigComposer.registerConfig(YourMod.MOD_ID, Config.class);
 * ConfigComposer.registerConfig(YourMod.MOD_ID, Config2.class);
 * ...
 * }</pre>
 *
 * <p>If a mod registers multiple config classes, the screen factory will show a
 * bridge/selector screen. Otherwise, it will open the individual config directly.</p>
 */
public final class ConfigComposer {

    /**
     * Registers a config class under the given mod ID.
     *
     * @param modId the mod ID that owns this config
     * @param configClazz the class annotated with {@link AutoConfig}
     */
    public static void registerConfig(String modId, Class<?> configClazz) {
        ConfigManager.init(FMLPaths.CONFIGDIR.get(), configClazz);

        // Tracks the class in the registrar (for screen creation)
        ConfigRegistry.register(modId, configClazz);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForgeConfigScreens.register(modId);
        }
    }

}
