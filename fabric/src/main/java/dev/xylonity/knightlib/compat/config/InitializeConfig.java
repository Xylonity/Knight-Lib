package dev.xylonity.knightlib.compat.config;

import dev.xylonity.knightlib.KnightLibCommon;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraftforge.fml.config.ModConfig;

public class InitializeConfig {

    public static void init() {
        ForgeConfigRegistry.INSTANCE.register(KnightLibCommon.MOD_ID, ModConfig.Type.COMMON, KnightLibConfig.SPEC, "knightlib.toml");
    }

}
