package dev.xylonity.knightlib.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class KnightLibConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Drop Chance Configurations
    public static final ModConfigSpec.DoubleValue DROP_CHANCE_SMALL_ESSENCE;

    static {
        // Drop Chance Configuration Section
        BUILDER.push("Drop Chance Configuration");
        DROP_CHANCE_SMALL_ESSENCE = BUILDER.comment("Drop chance for small essence").defineInRange("Drop chance for small essence", 0.25, 0, 1);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}