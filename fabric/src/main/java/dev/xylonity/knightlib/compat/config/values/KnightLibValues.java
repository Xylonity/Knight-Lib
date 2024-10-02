package dev.xylonity.knightlib.compat.config.values;

import dev.xylonity.knightlib.compat.config.FCAPChecker;
import dev.xylonity.knightlib.compat.config.KnightLibConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class KnightLibValues {

    static Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("knightquest.toml");
    private static final boolean V = Files.exists(CONFIG_PATH) && FCAPChecker.isLoaded();

    // Drop Chance Configuration Section
    public static float DROP_CHANCE_SMALL_ESSENCE  = (float) (V ? KnightLibConfig.DROP_CHANCE_SMALL_ESSENCE.get() : 0.25);

}
