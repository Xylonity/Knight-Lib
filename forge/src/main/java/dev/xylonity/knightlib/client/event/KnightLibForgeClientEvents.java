package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.bossbar.BossBarContext;
import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.*;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.client.event.impl.*;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderManager;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderStage;
import dev.xylonity.knightlib.client.screen.bossbar.BossBarApi;
import dev.xylonity.knightlib.client.screen.bossbar.BossBarLinks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Optional;

public class KnightLibForgeClientEvents {

    @Mod.EventBusSubscriber(modid = KnightLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KnightLibClientModBus {

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                final EntityRendererRegistrationEventForge rendererEvent = new EntityRendererRegistrationEventForge();
                KnightLibEvents.CLIENT.dispatch(rendererEvent);

                final BlockEntityRendererRegistrationEventForge beRendererEvent = new BlockEntityRendererRegistrationEventForge();
                KnightLibEvents.CLIENT.dispatch(beRendererEvent);

                final MenuScreenRegistrationEventForge menuScreenEvent = new MenuScreenRegistrationEventForge();
                KnightLibEvents.CLIENT.dispatch(menuScreenEvent);

                final BossBarRegistrationEvent bossBarEvent = new BossBarRegistrationEvent();
                KnightLibEvents.CLIENT.dispatch(bossBarEvent);

                final RenderLayerRegistrationEventForge renderLayerEvent = new RenderLayerRegistrationEventForge();
                KnightLibEvents.CLIENT.dispatch(renderLayerEvent);
            });

        }

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {
            final ParticleProviderRegistrationEventForge particleEvent = new ParticleProviderRegistrationEventForge();
            KnightLibEvents.CLIENT.dispatch(particleEvent);

            particleEvent.applyToForgeEvent(event);
        }

        @SubscribeEvent
        public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
            final AdditionalModelsRegistrationEventForge modelsEvent = new AdditionalModelsRegistrationEventForge();
            KnightLibEvents.CLIENT.dispatch(modelsEvent);

            modelsEvent.applyToForgeEvent(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            final KeyMappingRegistrationEventForge keyEvent = new KeyMappingRegistrationEventForge();
            KnightLibEvents.CLIENT.dispatch(keyEvent);

            keyEvent.applyToForgeEvent(event);
        }

        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) {
            KnightLibEvents.CLIENT.dispatch(new CustomPostShaderRegistrationEvent(PostShaderManager::register));

            final ShaderRegistrationEventForge shaderEvent = new ShaderRegistrationEventForge(event);
            KnightLibEvents.CLIENT.dispatch(shaderEvent);
        }

        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new SimplePreparableReloadListener<Void>() {

                @Override
                protected @NotNull Void prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                    return null;
                }

                @Override
                protected void apply(@NotNull Void prepared, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                    Minecraft minecraft = Minecraft.getInstance();
                    KnightLibEvents.CLIENT.dispatch(new ClientResourcesReloadedEvent(minecraft, resourceManager));
                }

            });

        }

    }

    @Mod.EventBusSubscriber(modid = KnightLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class KnightLibClientForgeBus {

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            final Minecraft minecraft = Minecraft.getInstance();

            if (event.phase == TickEvent.Phase.END) {
                KnightLibEvents.CLIENT.dispatch(new ClientTickEvent(minecraft, TickPhase.END));
            }
            else {
                KnightLibEvents.CLIENT.dispatch(new ClientTickEvent(minecraft, TickPhase.START));
            }

        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (!event.player.level().isClientSide()) {
                return;
            }

            final Minecraft minecraft = Minecraft.getInstance();
            final LocalPlayer localPlayer = minecraft.player;

            if (localPlayer == null || localPlayer != event.player) {
                return;
            }

            TickPhase phase = event.phase == TickEvent.Phase.END ? TickPhase.END : TickPhase.START;
            KnightLibEvents.CLIENT.dispatch(new ClientPlayerTickEvent(minecraft, localPlayer, phase));
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            KnightLibEvents.CLIENT.dispatch(new ClientKeyInputEvent(event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers()));
        }

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            KnightLibEvents.CLIENT.dispatch(new ItemStackTooltipEvent(
                    event.getItemStack(), event.getToolTip(), event.getFlags()
            ));

        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            float partialTick = minecraft.getFrameTime();

            KnightLibEvents.CLIENT.dispatch(new ClientRenderGuiEvent(minecraft, event.getGuiGraphics(), partialTick));
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            PostShaderRenderStage stage;
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                stage = PostShaderRenderStage.FRAME_BEGIN;
            }
            else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
                stage = PostShaderRenderStage.DEPTH_READY;
            }
            else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                stage = PostShaderRenderStage.AFTER_TRANSLUCENT_BLOCKS;
            }
            else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                stage = PostShaderRenderStage.AFTER_LEVEL;
            }
            else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                stage = PostShaderRenderStage.AFTER_ENTITIES;
            }
            else {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }

            float partialTick = event.getPartialTick();

            Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
            Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose());

            Vec3 cameraPosition = event.getCamera().getPosition();

            KnightLibEvents.CLIENT.dispatch(new ClientRenderLevelStageEvent(minecraft, stage, partialTick, projection, modelView, event.getCamera(), cameraPosition));
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            final Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientLogoutEvent(minecraft));
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen != null || minecraft.player == null) {
                return;
            }

            final ClientMouseScrollEvent scrollEvent = new ClientMouseScrollEvent(minecraft, event.getScrollDelta());
            KnightLibEvents.CLIENT.dispatch(scrollEvent);

            if (scrollEvent.isCancelled()) {
                event.setCanceled(true);
            }

        }

        @SubscribeEvent
        public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
            Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientComputeCameraAnglesEvent(minecraft, event.getCamera(), event.getCamera().getEntity(), (float) event.getPartialTick()));

            // Forge re-applies the event angles right after this hook, which would clobber any rotation set directly on the camera
            event.setYaw(event.getCamera().getYRot());
            event.setPitch(event.getCamera().getXRot());
        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (!event.getLevel().isClientSide()) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientEntityJoinLevelEvent(minecraft, event.getLevel(), event.getEntity()));
        }

        @SubscribeEvent
        public static void onEntityLeave(EntityLeaveLevelEvent event) {
            if (!event.getLevel().isClientSide()) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientEntityLeaveLevelEvent(minecraft, event.getLevel(), event.getEntity()));
        }

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientLoginEvent(minecraft));
        }

        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load event) {
            if (!event.getLevel().isClientSide()) {
                return;
            }

            final Level level = (Level) event.getLevel();
            final Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientWorldLoadEvent(minecraft, level));
        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (!event.getLevel().isClientSide()) {
                return;
            }

            final Level level = (Level) event.getLevel();
            final Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientWorldUnloadEvent(minecraft, level));
        }

        @SubscribeEvent
        public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
            LerpingBossEvent boss = event.getBossEvent();

            Optional<BossBarApi.BossBarEntry> match = BossBarApi.match(boss);
            if (match.isEmpty()) {
                return;
            }

            GuiGraphics gui = event.getGuiGraphics();
            int x = event.getX();
            int y = event.getY();

            event.setCanceled(true);

            BossBarLinks.Reference reference = BossBarLinks.INSTANCE.get(boss.getId());
            Entity entity = reference != null ? reference.resolve() : null;

            BossBarApi.BossBarEntry entry = match.get();
            BossBarContext context = new BossBarContext(boss, entity, reference != null ? reference.entityType() : null);

            if (entry.renderer() != null) {
                entry.renderer().render(gui, context, x, y);
            }
            else if (entry.legacyRenderer() != null) {
                entry.legacyRenderer().render(gui, boss, x, y);
            }

            if (entry.extraYPadding() != 0) {
                event.setIncrement(event.getIncrement() + entry.extraYPadding());
            }

        }

    }

}