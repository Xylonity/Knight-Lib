package dev.xylonity.knightlib.api.config;

import dev.xylonity.knightlib.config.interop.ConfigManager;
import dev.xylonity.knightlib.config.interop.ConfigRegistry;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entry point for the KnightLib configuration system.
 *
 * <h3>Usage from any mod:</h3>
 * <pre>{@code
 * // In your mod initializer (either server or client):
 * ConfigComposer.registerConfig(YourMod.MOD_ID, Config.class);
 * ConfigComposer.registerConfig(YourMod.MOD_ID, Config2.class);
 * ...
 * }</pre>
 *
 * <p>On Fabric, the config screen integration requires the mod "ModMenu".</p>
 */
public final class ConfigComposer {

    /**
     * Registers a config class under the given mod ID.
     *
     * @param modId the mod ID that owns this config
     * @param configClazz the class annotated with {@link AutoConfig}
     */
    public static void registerConfig(String modId, Class<?> configClazz) {
        ConfigManager.init(FabricLoader.getInstance().getConfigDir(), configClazz);
        ConfigRegistry.register(modId, configClazz);
    }

}