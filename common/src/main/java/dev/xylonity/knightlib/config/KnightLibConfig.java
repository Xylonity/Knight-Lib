package dev.xylonity.knightlib.config;

import dev.xylonity.knightlib.api.config.AutoConfig;
import dev.xylonity.knightlib.api.config.ConfigEntry;

@AutoConfig(
        file = "knightlib",
        title = "KnightLib Common Config",
        description = "Core config shared across KnightLib mods.",
        accentColor = 0xFFF0E545
)
public final class KnightLibConfig {

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Chance to drop small essence upon defeating an enemy",
            min = 0.0d,
            max = 1.0d,
            slider = true
    )
    public static double SMALL_ESSENCE_DROP_RATE = 0.125d;

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Chance to drop great essence upon defeating an enemy",
            min = 0.0d,
            max = 1.0d,
            slider = true
    )
    public static double GREAT_ESSENCE_DROP_RATE = 0.05d;

}
