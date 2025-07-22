package dev.xylonity.knightlib;

import dev.xylonity.knightlib.platform.KnightLibPlatform;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public class KnightLibCommon {

    public static final String MOD_ID = "knightlib";
    public static final Logger LOGGER = LoggerFactory.getLogger("KnightLib");

    public static final KnightLibPlatform PLATFORM = ServiceLoader.load(KnightLibPlatform.class).findFirst().orElseThrow();

    public static void init() {
        KnightLibItems.init();
        KnightLibBlocks.init();
        KnightLibParticles.init();
    }

}