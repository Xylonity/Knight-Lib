package dev.xylonity.knightlib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitbox;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxHolder;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxManager;
import dev.xylonity.knightlib.api.entity.hitbox.OBB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Shadow
    public abstract boolean shouldRenderHitBoxes();

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    ordinal = 0
            )
    )
    private <E extends Entity> void knightlib$renderOBBHitboxes(E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!shouldRenderHitBoxes() || entity.isInvisible() || Minecraft.getInstance().showOnlyReducedInfo()) {
            return;
        }

        final VertexConsumer lines = buffer.getBuffer(RenderType.lines());

        // Renders the bone hitbox OBBs from the given entity
        if (entity instanceof BoneHitboxHolder hitboxHolder) {
            final BoneHitboxManager manager = hitboxHolder.getBoneHitboxManager();
            if (manager != null && manager.isActive()) {
                manager.updateClientPose(partialTicks);
                for (final BoneHitbox hitbox : manager.getAll()) {
                    if (!hitbox.isEnabled()) {
                        continue;
                    }

                    final OBB obb = hitbox.getCurrentOBB();
                    if (obb == null) {
                        continue;
                    }

                    knightlib$renderOBBHitbox(poseStack, lines, obb,
                            entity.getX(), entity.getY(), entity.getZ(),
                            1.0f, 0.0f, 0.0f, 1.0f);
                }

            }

        }

    }

    @Unique
    private static void knightlib$renderOBBHitbox(PoseStack poseStack, VertexConsumer buffer, OBB obb, double entityX, double entityY, double entityZ, float red, float green, float blue, float alpha) {
        final Vec3[] worldCorners = obb.getCorners();
        final float[][] corners = new float[8][3];

        for (int i = 0; i < 8; i++) {
            corners[i][0] = (float) (worldCorners[i].x - entityX);
            corners[i][1] = (float) (worldCorners[i].y - entityY);
            corners[i][2] = (float) (worldCorners[i].z - entityZ);
        }

        final Matrix4f pose = poseStack.last().pose();
        final Matrix3f normal = poseStack.last().normal();

        // Bottom
        knightlib$renderEdge(buffer, pose, normal, corners[0], corners[1], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[1], corners[5], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[5], corners[4], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[4], corners[0], red, green, blue, alpha);

        // Top
        knightlib$renderEdge(buffer, pose, normal, corners[2], corners[3], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[3], corners[7], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[7], corners[6], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[6], corners[2], red, green, blue, alpha);

        // Vertical edges
        knightlib$renderEdge(buffer, pose, normal, corners[0], corners[2], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[1], corners[3], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[4], corners[6], red, green, blue, alpha);
        knightlib$renderEdge(buffer, pose, normal, corners[5], corners[7], red, green, blue, alpha);
    }

    @Unique
    private static void knightlib$renderEdge(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float[] a, float[] b, float red, float green, float blue, float alpha) {
        float nx = b[0] - a[0];
        float ny = b[1] - a[1];
        float nz = b[2] - a[2];
        final float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        buffer.vertex(pose, a[0], a[1], a[2]).color(red, green, blue, alpha).normal(normal, nx, ny, nz).endVertex();
        buffer.vertex(pose, b[0], b[1], b[2]).color(red, green, blue, alpha).normal(normal, nx, ny, nz).endVertex();
    }

}
