package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.api.camera.CameraShakeManager;
import dev.xylonity.knightlib.api.event.RegisterEvent;
import dev.xylonity.knightlib.api.event.impl.client.*;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.music.IBossMusicProvider;
import dev.xylonity.knightlib.api.scheduler.TickScheduler;
import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.particle.StarsetParticle;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.client.shader.post.internal.PostShaderManager;
import dev.xylonity.knightlib.client.shader.post.internal.PostShaderRenderContext;
import dev.xylonity.knightlib.impl.internal.BossMusicManager;
import dev.xylonity.knightlib.impl.internal.BossMusicRegistry;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class KnightLibClientEvents {

    @RegisterEvent(priority = 100)
    public static void onClientTick(final ClientTickEvent event) {
        if (event.getPhase() == TickPhase.START) {
            PostShaderManager.clientTick();
        }
        else {
            BossMusicManager.clientTick(event.getClient());

            Level level = event.getClient().level;
            if (level == null) {
                BossMusicRegistry.clear();
                BossMusicManager.clear();
                CameraShakeManager.clearAll();
                return;
            }

            TickScheduler.clean();
            TickScheduler.incrementTick(level);
            TickScheduler.processClientTasks(level);
            TickScheduler.processCommonTasks(level);
        }

    }

    @RegisterEvent(priority = 100)
    public static void onClientRenderLevelStage(final ClientRenderLevelStageEvent event) {
        PostShaderRenderContext context = new PostShaderRenderContext(
                event.getStage(),
                event.getPartialTick(),
                event.getProjection(),
                event.getModelView(),
                event.getCamera(),
                event.getCameraPosition()
        );

        PostShaderManager.renderStage(context);
    }

    @RegisterEvent(priority = 100)
    public static void onRenderGui(final ClientRenderGuiEvent event) {
        PostShaderManager.renderOverlay(event.guiGraphics(), event.partialTick());
    }

    @RegisterEvent(priority = 100)
    public static void onLogout(final ClientLogoutEvent event) {
        PostShaderManager.onLogout();
    }

    @RegisterEvent(priority = 100)
    public static void onJoin(final ClientEntityJoinLevelEvent event) {
        if (event.getEntity() instanceof IBossMusicProvider provider) {
            BossMusicRegistry.register(provider);
        }

    }

    @RegisterEvent(priority = 100)
    public static void onLeave(final ClientEntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof IBossMusicProvider provider) {
            BossMusicRegistry.unregister(provider);
        }

    }

    @RegisterEvent(priority = 100)
    public static void onLogin(final ClientLoginEvent event) {
        BossMusicRegistry.clear();
        BossMusicManager.clear();
        CameraShakeManager.clearAll();
    }

    @RegisterEvent(priority = 100)
    public static void onWorldUnload(final ClientWorldUnloadEvent event) {
        BossMusicRegistry.clear();
        BossMusicManager.clear();
        CameraShakeManager.clearAll();
        PostShaderManager.onLogout();
    }

    @RegisterEvent(priority = 100)
    public static void onCameraAngles(final ClientComputeCameraAnglesEvent event) {
        if (event.getCameraEntity() instanceof Player player) {
            CameraShakeManager.applyShakeIfPresent(player, event.getCamera(), event.getPartialTick());
        }

    }

    @RegisterEvent(priority = 100)
    public static void onResourcesReloaded(final ClientResourcesReloadedEvent event) {
        PostShaderManager.onLogout();

    }

    @RegisterEvent(priority = 100)
    public static void registerEntityRenderers(final EntityRendererRegistrationEvent event) {
        event.register(KnightLibEntities.GREAT_CHALICE_STARSET_RING.get(), GreatChaliceStarsetRingRenderer::new);
    }

    @RegisterEvent(priority = 100)
    public static void registerBlockEntityRenderers(final BlockEntityRendererRegistrationEvent event) {
        event.register(KnightLibBlockEntities.GREAT_CHALICE.get(), GreatChaliceRenderer::new);
    }

    @RegisterEvent(priority = 100)
    public static void registerParticleProviders(final ParticleProviderRegistrationEvent event) {
        event.register(KnightLibParticles.STARSET.get(), StarsetParticle.Provider::new);
    }

}