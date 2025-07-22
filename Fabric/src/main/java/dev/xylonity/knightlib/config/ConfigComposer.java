package dev.xylonity.knightlib.config;

import dev.xylonity.knightlib.config.impl.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigComposer {

    public static void registerConfig(Class<?> clazz) {
        ConfigManager.init(FabricLoader.getInstance().getConfigDir(), clazz);
    }

}