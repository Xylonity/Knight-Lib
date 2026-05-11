package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.*;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.client.event.impl.*;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderManager;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderStage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;

public final class KnightLibFabricClientEvents {

    private static Level lastLevel = null;

    public static void init() {
        onShaderRegistrationEvents();
        onEntityLoadOrUnloadEvents();
        onLogoutOrLoginEvents();
        onRenderGuiEvents();
        onClientTickEvents();
        onClientPlayerTickEvents();
        onRenderLevelEvents();
        onClientLevelTrackingEvents();
        onClientResourcesReloadEvents();
        onItemTooltipEvents();
    }

    public static void dispatchRegistrationEvents() {
        registerRenderers();
        registerBlockEntityRenderers();
        registerParticles();
        registerAdditionalModels();
        registerMenuScreens();
        registerRenderLayers();
        registerKeyMappings();
        registerBossBars();
    }

    private static void registerRenderers() {
        EntityRendererRegistrationEventFabric event = new EntityRendererRegistrationEventFabric();
        KnightLibEvents.CLIENT.dispatch(event);
    }

    private static void registerBlockEntityRenderers() {
        BlockEntityRendererRegistrationEventFabric event = new BlockEntityRendererRegistrationEventFabric();
        KnightLibEvents.CLIENT.dispatch(event);
    }

    private static void registerParticles() {
        ParticleProviderRegistrationEventFabric event = new ParticleProviderRegistrationEventFabric();
        KnightLibEvents.CLIENT.dispatch(event);
    }

    private static void registerAdditionalModels() {
        AdditionalModelsRegistrationEventFabric event = new AdditionalModelsRegistrationEventFabric();
        KnightLibEvents.CLIENT.dispatch(event);

        event.applyToFabric();
    }

    private static void registerMenuScreens() {
        MenuScreenRegistrationEventFabric event = new MenuScreenRegistrationEventFabric();
        KnightLibEvents.CLIENT.dispatch(event);
    }

    private static void registerRenderLayers() {
        RenderLayerRegistrationEventFabric event = new RenderLayerRegistrationEventFabric();
        KnightLibEvents.CLIENT.dispatch(event);
    }

    private static void registerKeyMappings() {
        KeyMappingRegistrationEventFabric event = new KeyMappingRegistrationEventFabric();
        KnightLibEvents.CLIENT.dispatch(event);
    }

    private static void registerPostShaders() {
        CustomPostShaderRegistrationEvent event = new CustomPostShaderRegistrationEvent(PostShaderManager::register);
        KnightLibEvents.CLIENT.dispatch(event);
    }

    private static void onShaderRegistrationEvents() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            registerPostShaders();

            ShaderRegistrationEventFabric event = new ShaderRegistrationEventFabric(context);
            KnightLibEvents.CLIENT.dispatch(event);
        });

    }

    private static void registerBossBars() {
        BossBarRegistrationEvent event = new BossBarRegistrationEvent();
        KnightLibEvents.CLIENT.dispatch(event);
    }

    private static void onEntityLoadOrUnloadEvents() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            final Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientEntityJoinLevelEvent(minecraft, level, entity));
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            final Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientEntityLeaveLevelEvent(minecraft, level, entity));
        });

    }

    private static void onLogoutOrLoginEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            KnightLibEvents.CLIENT.dispatch(new ClientLogoutEvent(client));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            KnightLibEvents.CLIENT.dispatch(new ClientLoginEvent(client));
        });

    }

    private static void onRenderGuiEvents() {
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            final Minecraft minecraft = Minecraft.getInstance();
            KnightLibEvents.CLIENT.dispatch(new ClientRenderGuiEvent(minecraft, guiGraphics, tickDelta));
        });

    }

    private static void onClientTickEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            KnightLibEvents.CLIENT.dispatch(new ClientTickEvent(client, TickPhase.START));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KnightLibEvents.CLIENT.dispatch(new ClientTickEvent(client, TickPhase.END));
        });

    }

    private static void onClientPlayerTickEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            final LocalPlayer player = client.player;
            if (player != null) {
                KnightLibEvents.CLIENT.dispatch(new ClientPlayerTickEvent(client, player, TickPhase.START));
            }

        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            final LocalPlayer player = client.player;
            if (player != null) {
                KnightLibEvents.CLIENT.dispatch(new ClientPlayerTickEvent(client, player, TickPhase.END));
            }

        });

    }

    private static void onRenderLevelEvents() {
        BiConsumer<WorldRenderContext, PostShaderRenderStage> dispatch =
                (context, stage) -> {
                    final Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.level == null) {
                        return;
                    }

                    final float partialTick = context.tickDelta();

                    final Matrix4f projection = new Matrix4f(context.projectionMatrix());
                    final Matrix4f modelView  = new Matrix4f(context.matrixStack().last().pose());

                    final Vec3 cameraPosition = context.camera().getPosition();

                    KnightLibEvents.CLIENT.dispatch(new ClientRenderLevelStageEvent(
                            minecraft,
                            stage,
                            partialTick,
                            projection,
                            modelView,
                            context.camera(),
                            cameraPosition
                    ));

                };

        WorldRenderEvents.START.register(context -> {
            dispatch.accept(context, PostShaderRenderStage.FRAME_BEGIN);
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            dispatch.accept(context, PostShaderRenderStage.DEPTH_READY);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            dispatch.accept(context, PostShaderRenderStage.AFTER_ENTITIES);
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            dispatch.accept(context, PostShaderRenderStage.AFTER_TRANSLUCENT_BLOCKS);
        });

        WorldRenderEvents.END.register(context -> {
            dispatch.accept(context, PostShaderRenderStage.AFTER_LEVEL);
        });
    }

    private static void onClientLevelTrackingEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            final Level current = client.level;

            if (current == lastLevel) {
                return;
            }

            if (lastLevel != null) {
                KnightLibEvents.CLIENT.dispatch(new ClientWorldUnloadEvent(client, lastLevel));
            }

            if (current != null) {
                KnightLibEvents.CLIENT.dispatch(new ClientWorldLoadEvent(client, current));
            }

            lastLevel = current;
        });

    }

    private static void onClientResourcesReloadEvents() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {

                    private static final ResourceLocation ID = KnightLib.of("client_resources_reloaded");

                    @Override
                    public ResourceLocation getFabricId() {
                        return ID;
                    }

                    @Override
                    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
                        Minecraft minecraft = Minecraft.getInstance();
                        KnightLibEvents.CLIENT.dispatch(new ClientResourcesReloadedEvent(minecraft, resourceManager));
                    }

                }

        );

    }

    private static void onItemTooltipEvents() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            KnightLibEvents.CLIENT.dispatch(new ItemStackTooltipEvent(stack, lines, context));
        });

    }

}