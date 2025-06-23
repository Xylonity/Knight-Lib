package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.common.api.TickScheduler;
import dev.xylonity.knightlib.common.particle.StarsetParticle;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.world.level.Level;

public final class KnightLibClientEvents {

    public static void init() {
        EntityRendererRegistry.register(KnightLibEntities.GREAT_CHALICE_STARSET_RING, GreatChaliceStarsetRingRenderer::new);

        BlockEntityRendererRegistry.register(KnightLibBlockEntities.GREAT_CHALICE, ctx -> new GreatChaliceRenderer(null));

        ParticleFactoryRegistry.getInstance().register(KnightLibParticles.STARSET.get(), StarsetParticle.Provider::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Level level = client.level;
            if (level == null) return;

            TickScheduler.clean();
            TickScheduler.incrementTick(level);
            TickScheduler.processClientTasks(level);
            TickScheduler.processCommonTasks(level);
        });
    }

}
