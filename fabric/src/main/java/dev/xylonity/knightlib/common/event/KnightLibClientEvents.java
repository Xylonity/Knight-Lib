package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.common.particle.StarsetParticle;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class KnightLibClientEvents {

    public static void init() {
        EntityRendererRegistry.register(KnightLibEntities.GREAT_CHALICE_STARSET_RING, GreatChaliceStarsetRingRenderer::new);

        BlockEntityRendererRegistry.register(KnightLibBlockEntities.GREAT_CHALICE, ctx -> new GreatChaliceRenderer(null));

        ParticleFactoryRegistry.getInstance().register(KnightLibParticles.STARSET.get(), StarsetParticle.Provider::new);
    }

}
