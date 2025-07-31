package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.CameraShakeManager;
import dev.xylonity.knightlib.api.IBossMusicProvider;
import dev.xylonity.knightlib.api.TickScheduler;
import dev.xylonity.knightlib.api.impl.BossBarApi;
import dev.xylonity.knightlib.api.impl.BossMusicRegistry;
import dev.xylonity.knightlib.api.internal.BossMusicManager;
import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.common.particle.StarsetParticle;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Optional;

public class KnightLibClientEvents {

    @Mod.EventBusSubscriber(modid = KnightLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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

    @Mod.EventBusSubscriber(modid = KnightLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class KnightLibClientForgeBus {

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

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
        public static void onLevelTick(TickEvent.LevelTickEvent e) {
            if (e.phase == TickEvent.Phase.END && e.level.isClientSide()) {
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

        @SubscribeEvent
        public static void onReload(RegisterClientReloadListenersEvent e) {
            BossMusicRegistry.clear();
            BossMusicManager.clear();
        }

        @SubscribeEvent
        public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
            LerpingBossEvent boss = event.getBossEvent();
            Optional<BossBarApi.BossBarEntry> match = BossBarApi.match(boss);

            if (match.isEmpty()) return;

            BossBarApi.BossBarEntry entry = match.get();

            GuiGraphics gui = event.getGuiGraphics();
            int x = event.getX();
            int y = event.getY();

            event.setCanceled(true);

            entry.renderer().render(gui, boss, x, y);

            if (entry.extraYPadding() != 0) {
                event.setIncrement(event.getIncrement() + entry.extraYPadding());
            }

            // hideVanillaName option temporary disabled
        }

    }

}