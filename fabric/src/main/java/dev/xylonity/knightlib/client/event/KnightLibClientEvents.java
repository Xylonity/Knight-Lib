package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.api.IBossMusicProvider;
import dev.xylonity.knightlib.api.impl.BossMusicRegistry;
import dev.xylonity.knightlib.api.internal.BossMusicManager;
import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.api.TickScheduler;
import dev.xylonity.knightlib.common.particle.StarsetParticle;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class KnightLibClientEvents {

    public static void init() {
        EntityRendererRegistry.register(KnightLibEntities.GREAT_CHALICE_STARSET_RING.get(), GreatChaliceStarsetRingRenderer::new);

        BlockEntityRendererRegistry.register(KnightLibBlockEntities.GREAT_CHALICE.get(), ctx -> new GreatChaliceRenderer(null));

        ParticleFactoryRegistry.getInstance().register(KnightLibParticles.STARSET.get(), StarsetParticle.Provider::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BossMusicManager.clientTick(Minecraft.getInstance());

            Level level = client.level;
            if (level == null) return;

            TickScheduler.clean();
            TickScheduler.incrementTick(level);
            TickScheduler.processClientTasks(level);
            TickScheduler.processCommonTasks(level);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            BossMusicRegistry.clear();
            if (client.level != null) {
                for (Entity e : client.level.entitiesForRendering()) {
                    if (e instanceof IBossMusicProvider prov) {
                        BossMusicRegistry.register(prov);
                    }
                }
            }

        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BossMusicRegistry.clear();
            BossMusicManager.clear();
        });

    }

}
