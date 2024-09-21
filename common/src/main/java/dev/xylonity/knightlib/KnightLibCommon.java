package dev.xylonity.knightlib;

import dev.xylonity.knightlib.compat.registry.KnightLibItems;
import dev.xylonity.knightlib.platform.KnightLibPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Common mod loader class.
 */
public class KnightLibCommon {

    public static final String MOD_ID = "knightlib";
    public static final Logger LOGGER = LoggerFactory.getLogger("Knight Lib");

    public static final KnightLibPlatform COMMON_PLATFORM = ServiceLoader.load(KnightLibPlatform.class).findFirst().orElseThrow();

    public static void init() {
        KnightLibItems.init();
    }

}