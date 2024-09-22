package dev.xylonity.knightlib.compat.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class KnightLibConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Drop Chance Configurations
    public static final ForgeConfigSpec.DoubleValue DROP_CHANCE_SMALL_ESSENCE;

    static {
        // Drop Chance Configuration Section
        BUILDER.push("Drop Chance Configuration");
        DROP_CHANCE_SMALL_ESSENCE = BUILDER.comment("Drop chance for small essence").defineInRange("Drop chance for small essence", 0.15, 0, 1);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}