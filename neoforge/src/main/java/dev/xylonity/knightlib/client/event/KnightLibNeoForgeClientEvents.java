package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.bossbar.BossBarContext;
import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.*;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.client.event.impl.*;
import dev.xylonity.knightlib.client.item.renderer.GenericBlockItemRenderer;
import dev.xylonity.knightlib.client.screen.bossbar.BossBarApi;
import dev.xylonity.knightlib.client.screen.bossbar.BossBarLinks;
import dev.xylonity.knightlib.common.item.blockitem.GenericBlockItem;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderManager;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Optional;

public class KnightLibNeoForgeClientEvents {

    @EventBusSubscriber(modid = KnightLib.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KnightLibClientModBus {

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            EntityRendererRegistrationEventNeoForge rendererEvent = new EntityRendererRegistrationEventNeoForge();
            KnightLibEvents.CLIENT.dispatch(rendererEvent);

            BlockEntityRendererRegistrationEventNeoForge beRendererEvent = new BlockEntityRendererRegistrationEventNeoForge();
            KnightLibEvents.CLIENT.dispatch(beRendererEvent);

            BossBarRegistrationEvent bossBarEvent = new BossBarRegistrationEvent();
            KnightLibEvents.CLIENT.dispatch(bossBarEvent);

            RenderLayerRegistrationEventNeoForge renderLayerEvent = new RenderLayerRegistrationEventNeoForge();
            KnightLibEvents.CLIENT.dispatch(renderLayerEvent);
        }

        @SubscribeEvent
        public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
            MenuScreenRegistrationEventNeoForge menuScreenEvent = new MenuScreenRegistrationEventNeoForge();
            KnightLibEvents.CLIENT.dispatch(menuScreenEvent);

            menuScreenEvent.applyToForgeEvent(event);
        }

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {
            ParticleProviderRegistrationEventNeoForge particleEvent = new ParticleProviderRegistrationEventNeoForge();
            KnightLibEvents.CLIENT.dispatch(particleEvent);

            particleEvent.applyToForgeEvent(event);
        }

        @SubscribeEvent
        public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
            AdditionalModelsRegistrationEventNeoForge modelsEvent = new AdditionalModelsRegistrationEventNeoForge();
            KnightLibEvents.CLIENT.dispatch(modelsEvent);

            modelsEvent.applyToForgeEvent(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            KeyMappingRegistrationEventNeoForge keyEvent = new KeyMappingRegistrationEventNeoForge();
            KnightLibEvents.CLIENT.dispatch(keyEvent);

            keyEvent.applyToForgeEvent(event);
        }

        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) {
            KnightLibEvents.CLIENT.dispatch(new CustomPostShaderRegistrationEvent(PostShaderManager::register));

            ShaderRegistrationEventNeoForge shaderEvent = new ShaderRegistrationEventNeoForge(event);
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

    @EventBusSubscriber(modid = KnightLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class KnightLibClientForgeBus {

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onClientTickPre(ClientTickEvent.Pre event) {
            KnightLibEvents.CLIENT.dispatch(new dev.xylonity.knightlib.api.event.impl.client.ClientTickEvent(Minecraft.getInstance(), TickPhase.START));
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onClientTickPost(ClientTickEvent.Post event) {
            KnightLibEvents.CLIENT.dispatch(new dev.xylonity.knightlib.api.event.impl.client.ClientTickEvent(Minecraft.getInstance(), TickPhase.END));
        }

        @SubscribeEvent
        public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
            dispatchPlayerTick(event, TickPhase.START);
        }

        @SubscribeEvent
        public static void onPlayerTickPost(PlayerTickEvent.Post event) {
            dispatchPlayerTick(event, TickPhase.END);
        }

        private static void dispatchPlayerTick(PlayerTickEvent event, TickPhase phase) {
            if (!event.getEntity().level().isClientSide()) {
                return;
            }

            final Minecraft minecraft = Minecraft.getInstance();
            final LocalPlayer localPlayer = minecraft.player;

            if (localPlayer == null || localPlayer != event.getEntity()) {
                return;
            }

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
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

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

            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

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
        public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
            Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientComputeCameraAnglesEvent(minecraft, event.getCamera(), event.getCamera().getEntity(), (float) event.getPartialTick()));
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