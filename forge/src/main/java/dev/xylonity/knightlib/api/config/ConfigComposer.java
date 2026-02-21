package dev.xylonity.knightlib.api.config;

import dev.xylonity.knightlib.config.interop.ConfigManager;
import dev.xylonity.knightlib.config.interop.ConfigRegistry;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge entry point for the KnightLib configuration system.
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

        // Registers the Forge config screen factory
        registerScreenFactory(modId);
    }

    private static void registerScreenFactory(String modId) {
        try {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, screen) -> ConfigRegistry.createScreen(modId, screen)
                    )

            );

        }
        catch (Exception ignored) {
            ;;
        }

    }

}