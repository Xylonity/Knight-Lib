package dev.xylonity.knightlib.compat.config;

import dev.xylonity.knightlib.KnightLibCommon;
import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeConfigRegistry;
import net.neoforged.fml.config.ModConfig;

public class InitializeConfig {

    public static void init() {
        ForgeConfigRegistry.INSTANCE.register(KnightLibCommon.MOD_ID, ModConfig.Type.COMMON, KnightLibConfig.SPEC, "knightlib.toml");
    }

}
