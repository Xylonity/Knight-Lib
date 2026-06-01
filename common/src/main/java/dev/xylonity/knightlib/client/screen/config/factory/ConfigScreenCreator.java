package dev.xylonity.knightlib.client.screen.config.factory;

import dev.xylonity.knightlib.client.screen.config.ConfigBridgeScreen;
import dev.xylonity.knightlib.client.screen.config.KnightLibConfigScreen;
import dev.xylonity.knightlib.config.interop.ConfigRegistry;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class ConfigScreenCreator {

    /**
     * Creates the appropriate config screen for a mod.
     * If the mod has a single config, returns the config editor directly.
     * If the mod has multiple configs, returns a selector screen.
     */
    public static Screen createScreen(String modId, Screen parent) {
        List<Class<?>> configs = ConfigRegistry.getConfigs(modId);
        if (configs.isEmpty()) {
            return parent;
        }

        if (configs.size() == 1) {
            return new KnightLibConfigScreen(parent, configs.get(0));
        }

        return new ConfigBridgeScreen(parent, modId, configs);
    }

}