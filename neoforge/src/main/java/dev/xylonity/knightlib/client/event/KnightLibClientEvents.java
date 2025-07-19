package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.CameraShakeManager;
import dev.xylonity.knightlib.api.IBossMusicProvider;
import dev.xylonity.knightlib.api.TickScheduler;
import dev.xylonity.knightlib.api.impl.BossMusicRegistry;
import dev.xylonity.knightlib.api.internal.BossMusicManager;
import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.common.particle.StarsetParticle;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class KnightLibClientEvents {

    @EventBusSubscriber(modid = KnightLib.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KnightLibClientModBus {

        @SubscribeEvent
        public static void registerEntityRenderers(FMLClientSetupEvent event) {
            EntityRenderers.register(KnightLibEntities.GREAT_CHALICE_STARSET_RING.get(), GreatChaliceStarsetRingRenderer::new);

            BlockEntityRenderers.register(KnightLibBlockEntities.GREAT_CHALICE.get(), GreatChaliceRenderer::new);
        }

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(KnightLibParticles.STARSET.get(), StarsetParticle.Provider::new);
        }

    }

    @EventBusSubscriber(modid = KnightLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class KnightLibClientForgeBus {

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            BossMusicManager.clientTick(minecraft);

            Level level = minecraft.level;
            if (level == null) return;

            TickScheduler.clean();
            TickScheduler.incrementTick(level);
            TickScheduler.processClientTasks(level);
            TickScheduler.processCommonTasks(level);
        }

        @SubscribeEvent
        public static void onLevelTick(LevelTickEvent.Post e) {
            if (e.getLevel().isClientSide()) {
                CameraShakeManager.clear();
            }

        }

        @SubscribeEvent
        public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
            if (e.getCamera().getEntity() instanceof Player p) {
                CameraShakeManager.applyShakeIfPresent(p, e.getCamera());
            }

        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent e) {
            if (!e.getLevel().isClientSide()) return;
            if (e.getEntity() instanceof IBossMusicProvider prov) {
                BossMusicRegistry.register(prov);
            }

        }

        @SubscribeEvent
        public static void onEntityLeave(EntityLeaveLevelEvent e) {
            if (!e.getLevel().isClientSide()) return;
            if (e.getEntity() instanceof IBossMusicProvider prov) {
                BossMusicRegistry.unregister(prov);
            }

        }

        @SubscribeEvent
        public static void onWorldUnload(LevelEvent.Unload e) {
            if (e.getLevel().isClientSide()) {
                BossMusicRegistry.clear();
                BossMusicManager.clear();
            }

        }

    }

}