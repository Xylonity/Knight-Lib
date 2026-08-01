package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathManager;
import dev.xylonity.knightlib.api.camera.shake.CameraShakeManager;
import dev.xylonity.knightlib.api.animation.KnightLibItemAnimations;
import dev.xylonity.knightlib.api.event.RegisterEvent;
import dev.xylonity.knightlib.client.camera.editor.CameraPathEditor;
import dev.xylonity.knightlib.client.camera.editor.EditorPathRenderer;
import dev.xylonity.knightlib.client.animation.AnimationSyncClient;
import dev.xylonity.knightlib.client.entity.data.internal.AttachmentsClient;
import dev.xylonity.knightlib.client.screen.bossbar.BossBarLinks;
import dev.xylonity.knightlib.api.event.impl.client.*;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.sound.music.IBossMusicProvider;
import dev.xylonity.knightlib.api.sound.persistent.KnightLibPersistentSounds;
import dev.xylonity.knightlib.api.scheduler.TickScheduler;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderManager;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderContext;
import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.particle.StarsetParticle;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.client.sound.music.internal.BossMusicManager;
import dev.xylonity.knightlib.client.sound.music.internal.BossMusicRegistry;
import dev.xylonity.knightlib.client.sound.persistent.internal.PersistentSoundPacketClientHandler;
import dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures;
import dev.xylonity.knightlib.network.packets.AnimationSyncS2C;
import dev.xylonity.knightlib.network.packets.AttachmentSyncS2C;
import dev.xylonity.knightlib.network.packets.BossBarLinkS2C;
import dev.xylonity.knightlib.network.packets.CameraPathS2C;
import dev.xylonity.knightlib.network.packets.CameraShakeS2C;
import dev.xylonity.knightlib.network.packets.PersistentSoundTickS2C;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

public class KnightLibClientEvents {

    private static KeyMapping campathEditorKey;
    private static KeyMapping campathPreviewKey;

    public static KeyMapping campathEditorKey() {
        return campathEditorKey;
    }

    public static KeyMapping campathPreviewKey() {
        return campathPreviewKey;
    }

    @RegisterEvent(priority = 100)
    public static void registerPacketHandlers(final ClientPacketHandlerRegistrationEvent event) {
        event.register(AnimationSyncS2C.class, AnimationSyncClient::handle);
        event.register(AttachmentSyncS2C.class, AttachmentsClient::handle);
        event.register(BossBarLinkS2C.class, message -> BossBarLinks.INSTANCE.put(message.bossEventId(), new BossBarLinks.Reference(message.entityId(), message.entityUuid(), message.entityType(), message.dimension())));
        event.register(CameraPathS2C.class, message -> {
            if (message.path() == null) {
                CameraPathManager.stop();
            }
            else {
                CameraPathManager.play(message.path());
            }

        });
        event.register(CameraShakeS2C.class, message -> CameraShakeManager.shake(message.spec(), message.replace()));
        event.register(PersistentSoundTickS2C.class, PersistentSoundPacketClientHandler::handle);
    }

    @RegisterEvent(priority = 100)
    public static void onClientTick(final ClientTickEvent event) {
        if (event.getPhase() == TickPhase.START) {
            PostShaderManager.clientTick();
        }
        else {
            AnimationSyncClient.tick();
            BossMusicManager.clientTick(event.getClient());
            handleCameraEditorKeys(event.getClient());

            Level level = event.getClient().level;
            if (level == null) {
                BossMusicRegistry.clear();
                BossMusicManager.clear();
                CameraShakeManager.clearAll();
                CameraPathManager.clearAll();
                CameraPathEditor.discard();
                KnightLibPersistentSounds.clearAll();
                AttachmentsClient.clearAll();
                BossBarLinks.INSTANCE.clear();
                AnimationSyncClient.clear();
                KnightLibItemAnimations.clear();
                return;
            }

            CameraPathManager.tick();
            CameraPathEditor.tick();

            TickScheduler.clean();
            TickScheduler.processClientTasks(level);
            TickScheduler.processCommonTasks(level);

            KnightLibPersistentSounds.endClientTick();
        }

    }

    /**
     * CamPath editor keybinds (editor and preview)
     */
    private static void handleCameraEditorKeys(final Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        if (campathEditorKey != null) {
            while (campathEditorKey.consumeClick()) {
                CameraPathEditor.toggle();
            }

        }

        if (campathPreviewKey != null) {
            while (campathPreviewKey.consumeClick()) {
                CameraPathEditor.togglePreview();
            }

        }

    }

    @RegisterEvent(priority = 100)
    public static void registerKeyMappings(final KeyMappingRegistrationEvent event) {
        // Campath editor is a dev only util, no keybinds are registered on real deploy
        if (!KnightLib.PLATFORM.isDevelopmentEnvironment()) {
            return;
        }

        campathEditorKey = new KeyMapping("key.knightlib.campath_editor", GLFW.GLFW_KEY_B, "key.categories.knightlib");
        campathPreviewKey = new KeyMapping("key.knightlib.campath_preview", GLFW.GLFW_KEY_N, "key.categories.knightlib");

        event.registerKeyMappings(campathEditorKey, campathPreviewKey);
    }

    @RegisterEvent(priority = 100)
    public static void onClientRenderLevelStage(final ClientRenderLevelStageEvent event) {
        final PostShaderRenderContext context = new PostShaderRenderContext(
                event.getStage(),
                event.getPartialTick(),
                event.getProjection(),
                event.getModelView(),
                event.getCamera(),
                event.getCameraPosition()
        );

        PostShaderManager.renderStage(context);
        EditorPathRenderer.onRenderStage(event);
    }

    @RegisterEvent(priority = 100)
    public static void onRenderGui(final ClientRenderGuiEvent event) {
        PostShaderManager.renderOverlay(event.guiGraphics(), event.partialTick());
    }

    @RegisterEvent(priority = 100)
    public static void onLogout(final ClientLogoutEvent event) {
        PostShaderManager.onLogout();
        KnightLibItemAnimations.clear();
    }

    @RegisterEvent(priority = 100)
    public static void onJoin(final ClientEntityJoinLevelEvent event) {
        if (event.getEntity() instanceof IBossMusicProvider provider) {
            BossMusicRegistry.register(provider);
        }

        AttachmentsClient.onEntityJoin(event.getEntity());
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
        CameraPathManager.clearAll();
        CameraPathEditor.discard();
        AttachmentsClient.clearAll();
        BossBarLinks.INSTANCE.clear();
        AnimationSyncClient.clear();
        KnightLibItemAnimations.clear();
    }

    @RegisterEvent(priority = 100)
    public static void onWorldUnload(final ClientWorldUnloadEvent event) {
        BossMusicRegistry.clear();
        BossMusicManager.clear();
        CameraShakeManager.clearAll();
        CameraPathManager.clearAll();
        CameraPathEditor.discard();
        KnightLibPersistentSounds.clearAll();
        AttachmentsClient.clearAll();
        BossBarLinks.INSTANCE.clear();
        AnimationSyncClient.clear();
        KnightLibItemAnimations.clear();
        PostShaderManager.onWorldUnload();

        TickScheduler.markForClean(event.getLevel());
        TickScheduler.clean();
    }

    @RegisterEvent(priority = 100)
    public static void onCameraAngles(final ClientComputeCameraAnglesEvent event) {
        if (event.getCameraEntity() instanceof Player player) {
            CameraPathManager.applyIfPresent(player, event.getCamera(), event.getPartialTick());
            CameraPathEditor.applyCamera(event.getCamera(), event.getPartialTick());
            CameraShakeManager.applyShakeIfPresent(player, event.getCamera(), event.getPartialTick());
        }

    }

    @RegisterEvent(priority = 100)
    public static void onResourcesReloaded(final ClientResourcesReloadedEvent event) {
        final Minecraft minecraft = event.getClient();
        KnightLibAnimatedTextures.onResourceReload();
        PostShaderManager.onResourcesReloaded(
                minecraft.getTextureManager(),
                event.getResourceManager(),
                minecraft.getMainRenderTarget()
        );

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