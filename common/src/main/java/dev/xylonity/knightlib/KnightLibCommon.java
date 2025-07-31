package dev.xylonity.knightlib;

import dev.xylonity.knightlib.registry.*;
import dev.xylonity.knightlib.platform.KnightLibPlatform;
import dev.xylonity.knightlib.platform.KnightLibRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public class KnightLibCommon {

    public static final String MOD_ID = "knightlib";
    public static final Logger LOGGER = LoggerFactory.getLogger("KnightLib");

    public static final KnightLibPlatform PLATFORM = ServiceLoader.load(KnightLibPlatform.class).findFirst().orElseThrow();
    public static final KnightLibRegistrar REGISTRAR = ServiceLoader.load(KnightLibRegistrar.class).findFirst().orElseThrow();

    public static void init() {
        KnightLibEntities.ENTITIES.init();
        KnightLibBlocks.BLOCKS.init();
        KnightLibItems.ITEMS.init();
        KnightLibParticles.PARTICLES.init();
        KnightLibBlockEntities.BLOCK_ENTITIES.init();
    }

}