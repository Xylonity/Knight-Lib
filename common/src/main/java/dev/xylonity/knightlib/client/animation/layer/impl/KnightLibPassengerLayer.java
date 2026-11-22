package dev.xylonity.knightlib.client.animation.layer.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xylonity.knightlib.api.entity.passenger.KnightLibPassengerHost;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayer;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayerContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Render layer that draws passengers of a {@link KnightLibPassengerHost} at a seat bone, following its animation. KnightLib already
 * skips those passengers in a rendering pass. The camera passenger is skipped in first person, like vanilla does
 */
public class KnightLibPassengerLayer<T extends Entity & KnightLibPassengerHost> extends KnightLibRenderLayer<T> {

    /// Nesting depth of passenger renders drawn without their held items, read by the held item layer
    private static int hidingHeldItems = 0;

    private final String seatBone;

    public KnightLibPassengerLayer(String seatBone) {
        this.seatBone = Objects.requireNonNull(seatBone, "seatBone");
    }

    /**
     * Whether held items are currently skipped, because a passenger is drawn without them
     */
    @ApiStatus.Internal
    public static boolean isHidingHeldItems() {
        return hidingHeldItems > 0;
    }

    /**
     * Bone the passenger sits at, by default the one given to the constructor
     */
    protected String seatBone(T vehicle, Entity passenger) {
        return seatBone;
    }

    /**
     * Whether the passenger is drawn with its held items
     */
    protected boolean renderHeldItems(T vehicle, Entity passenger) {
        return true;
    }

    /**
     * Moves the pose from the seat bone pivot to where the passenger's renderer starts (its feet)
     */
    protected void transformPassenger(KnightLibRenderLayerContext<T> context, Entity passenger, PoseStack poseStack) {
        if (passenger instanceof LivingEntity) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rendererBodyYaw(passenger, context.partialTick()) + 180f));
        }

    }

    /**
     * Body yaw a living passenger's renderer turns it by
     */
    protected float rendererBodyYaw(Entity passenger, float partialTick) {
        if (passenger.getVehicle() instanceof LivingEntity vehicle) {
            return Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
        }
        if (passenger instanceof LivingEntity living) {
            return Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
        }

        return 0f;
    }

    @Override
    public boolean shouldRender(KnightLibRenderLayerContext<T> context) {
        return context.target().isVehicle();
    }

    @Override
    public void render(KnightLibRenderLayerContext<T> context) {
        final T vehicle = context.target();
        final Minecraft minecraft = Minecraft.getInstance();
        final EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        final float partialTick = context.partialTick();
        for (final Entity passenger : vehicle.getPassengers()) {
            if (!vehicle.rendersPassenger(passenger) || isFirstPersonCamera(minecraft, passenger)) {
                continue;
            }

            final EntityRenderer<? super Entity> renderer = dispatcher.getRenderer(passenger);
            final boolean hideHeldItems = !renderHeldItems(vehicle, passenger);
            context.withBone(seatBone(vehicle, passenger), poseStack -> {
                transformPassenger(context, passenger, poseStack);
                if (hideHeldItems) {
                    hidingHeldItems++;
                }

                try {
                    renderer.render(passenger, 0f, partialTick, poseStack, context.buffers(), dispatcher.getPackedLightCoords(passenger, partialTick));
                }
                finally {
                    if (hideHeldItems) {
                        hidingHeldItems--;
                    }

                }

            });

        }

    }

    private boolean isFirstPersonCamera(Minecraft minecraft, Entity passenger) {
        final Camera camera = minecraft.gameRenderer.getMainCamera();
        return passenger == camera.getEntity() && !camera.isDetached();
    }

}