package dev.xylonity.knightlib;

import dev.xylonity.knightlib.compat.config.InitializeConfig;
import dev.xylonity.knightlib.compat.datagen.KnightLibLootModifierGenerator;
import dev.xylonity.knightlib.compat.particle.StarsetParticle;
import dev.xylonity.knightlib.compat.registry.KnightLibBlocks;
import dev.xylonity.knightlib.compat.registry.KnightLibParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.loader.api.FabricLoader;

public class KnightLib implements ModInitializer, ClientModInitializer {

    private static final String KNIGHTQUEST_MOD_ID = "knightquest";
    private static final String FCAP_MOD_ID = "forgeconfigapiport";

    @Override
    public void onInitialize() {

        KnightLibLootModifierGenerator.modifyLootTables();
        KnightLibParticles.register();
        KnightLibBlocks.register();

        if (isFCAPLoaded())
            InitializeConfig.init();

        KnightLibCommon.init();
    }

    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(KnightLibParticles.STARSET_PARTICLE, StarsetParticle.Provider::new);
    }

    /**
     * Verifies if the `Knight Quest` mod is loaded.
     *
     * @return true if it is loaded, false if not.
     */
    public static boolean isKnightQuestLoaded() {
        return FabricLoader.getInstance().isModLoaded(KNIGHTQUEST_MOD_ID);
    }

    /**
     * Verifies if the `Knight Quest` mod is loaded.
     *
     * @return true if it is loaded, false if not.
     */
    public static boolean isFCAPLoaded() {
        return FabricLoader.getInstance().isModLoaded(FCAP_MOD_ID);
    }

}
