package dev.xylonity.knightlib.client.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.*;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.client.shader.post.internal.PostShaderRenderStage;
import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.client.particle.StarsetParticle;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
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
        EntityRendererRegistry.register(
                KnightLibEntities.GREAT_CHALICE_STARSET_RING.get(),
                GreatChaliceStarsetRingRenderer::new
        );

        BlockEntityRendererRegistry.register(
                KnightLibBlockEntities.GREAT_CHALICE.get(),
                GreatChaliceRenderer::new
        );

        ParticleFactoryRegistry.getInstance().register(
                KnightLibParticles.STARSET.get(),
                StarsetParticle.Provider::new
        );

        onEntityLoadOrUnloadEvents();
        onLogoutOrLoginEvents();
        onRenderGuiEvents();
        onClientTickEvents();
        onRenderLevelEvents();
        onClientLevelTrackingEvents();
        onClientResourcesReloadEvents();
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

}