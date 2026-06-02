package dev.xylonity.knightlib.client.screen.config;

import dev.xylonity.knightlib.client.screen.config.factory.ConfigScreenCreator;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only holder for the NeoForge config screen extension point
 */
public final class NeoForgeConfigScreens {

    private NeoForgeConfigScreens() {
        ;;
    }

    public static void register(String modId) {
        try {
            ModLoadingContext.get().registerExtensionPoint(
                    IConfigScreenFactory.class,
                    () -> (container, modListScreen) -> ConfigScreenCreator.createScreen(modId, modListScreen)
            );
        }
        catch (Throwable ignored) {
            ;;
        }

    }

}
