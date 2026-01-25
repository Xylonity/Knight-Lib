package dev.xylonity.knightlib;

import dev.xylonity.knightlib.api.network.NetworkEndpoint;
import dev.xylonity.knightlib.network.NetworkInternal;
import dev.xylonity.knightlib.platform.KnightLibNetwork;
import dev.xylonity.knightlib.proxy.IProxy;
import dev.xylonity.knightlib.registry.*;
import dev.xylonity.knightlib.platform.KnightLibPlatform;
import dev.xylonity.knightlib.platform.KnightLibRegistrar;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.ServiceLoader;

public class KnightLib {

    public static final String MOD_ID = "knightlib";
    public static final Logger LOGGER = LoggerFactory.getLogger("KnightLib");

    public static final KnightLibPlatform PLATFORM = ServiceLoader.load(KnightLibPlatform.class).findFirst().orElseThrow();
    public static final KnightLibRegistrar REGISTRAR = ServiceLoader.load(KnightLibRegistrar.class).findFirst().orElseThrow();
    public static final KnightLibNetwork NETWORK = ServiceLoader.load(KnightLibNetwork.class).findFirst().orElseThrow();

    public static final NetworkEndpoint NET = NetworkInternal.knightlib();

    private static volatile EnumSet<Usage> ENABLED = EnumSet.noneOf(Usage.class);

    public static IProxy PROXY;

    protected static void init() {
        KnightLibEntities.ENTITIES.init();
        KnightLibBlocks.BLOCKS.init();
        KnightLibItems.ITEMS.init();
        KnightLibParticles.PARTICLES.init();
        KnightLibBlockEntities.BLOCK_ENTITIES.init();
    }

    public static ResourceLocation of(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /**
     * Opt-in entrypoint for mod consumers of KnightLib items and blocks.
     * <br>
     * <br>
     * This is done to prevent a mod that uses knightlib utilities, and NOT its items or blocks,
     * from populating the modpack with unusable content.
     * <br>
     * <br>
     * Pass one or more {@link Usage} usages to declare what your mod actually uses.
     * By default, all usages are <b>disabled</b>. Once a usage is claimed here, it becomes
     * available in the whole modpack. If {@link Usage#ALL} is provided, every usage is considered
     * enabled when checking.
     * <br>
     * <br>
     * Call this during your mod's initialization so any bridge (recipes, drops, etc.)
     * can honor it from startup.
     */
    public static void initialize(Usage... usages) {
        if (usages == null || usages.length == 0) return;

        synchronized (KnightLib.class) {
            EnumSet<Usage> next = ENABLED.isEmpty() ? EnumSet.noneOf(Usage.class) : EnumSet.copyOf(ENABLED);

            boolean changed = false;
            for (Usage usage : usages) {
                if (!next.contains(usage)) {
                    next.add(usage);

                    changed = true;

                    LOGGER.info("The following content usage has been enabled: {}", usage);
                }
            }

            if (changed) {
                ENABLED = next;
            }
        }

    }

    /**
     * Use this if your mod relies on all KnightLib items and blocks.
     */
    public static void initialize() {
        initialize(Usage.ALL);
    }

    public static boolean isEnabled(Usage usage) {
        EnumSet<Usage> should = ENABLED;
        return should.contains(Usage.ALL) || should.contains(usage);
    }

    public enum Usage {
        ALL,
        GREEN_ESSENCES,
        GREAT_CHALICE,
        COPPER_GRAILS,
        HOMUNCULUS
    }

}