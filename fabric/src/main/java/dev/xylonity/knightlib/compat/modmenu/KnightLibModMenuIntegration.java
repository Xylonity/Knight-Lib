package dev.xylonity.knightlib.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.config.interop.ConfigRegistry;

import java.util.HashMap;
import java.util.Map;

public class KnightLibModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> ConfigRegistry.createScreen(KnightLib.MOD_ID, screen);
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        // Provides screens for all mods that are registered via the ConfigComposer
        Map<String, ConfigScreenFactory<?>> screenFactoryHashMap = new HashMap<>();
        for (String modId : ConfigRegistry.getRegisteredMods()) {
            screenFactoryHashMap.put(modId, screen -> ConfigRegistry.createScreen(modId, screen));
        }

        return screenFactoryHashMap;
    }

}