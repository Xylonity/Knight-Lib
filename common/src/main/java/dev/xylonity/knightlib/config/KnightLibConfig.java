package dev.xylonity.knightlib.config;

import dev.xylonity.knightlib.config.api.*;

@AutoConfig(file = "knightlib")
public final class KnightLibConfig {

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Chance to drop small essence upon defeating an enemy",
            min = 0.0d,
            max = 1.0d
    )
    public static double SMALL_ESSENCE_DROP_RATE = 0.10d;

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Chance to drop great essence upon defeating an enemy",
            min = 0.0d,
            max = 1.0d
    )
    public static double GREAT_ESSENCE_DROP_RATE = 0.05d;

}
