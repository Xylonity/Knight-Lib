package dev.xylonity.knightlib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathManager;
import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.ClientComputeCameraRollEvent;
import dev.xylonity.knightlib.api.event.impl.client.ClientComputeFovEvent;
import dev.xylonity.knightlib.client.camera.editor.CameraPathEditor;
import dev.xylonity.knightlib.client.entity.layer.BoneHitboxPicker;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderManager;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderContext;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderStage;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Final @Shadow Minecraft minecraft;
    @Final @Shadow private Camera mainCamera;
    @Unique private float knightlib$cameraRoll = 0f;

    @Unique private final Matrix4f knightlib$levelProjection = new Matrix4f();
    @Unique private boolean knightlib$levelProjectionCaptured = false;

    @Shadow
    private double getFov(Camera camera, float partialTick, boolean useFovSetting) {
        throw new AssertionError();
    }

    @Inject(method = "pick", at = @At("TAIL"))
    private void knightlib$pickBoneHitboxes(float partialTicks, CallbackInfo ci) {
        BoneHitboxPicker.pick(minecraft, partialTicks);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void knightlib$computeFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        final double fov = cir.getReturnValueD();
        final ClientComputeFovEvent event = new ClientComputeFovEvent(minecraft, camera, partialTick, useFovSetting, fov);
        KnightLibEvents.CLIENT.dispatch(event);
        if (event.getFov() != fov) {
            cir.setReturnValue(event.getFov());
        }

    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void knightlib$computeCameraRoll(DeltaTracker deltaTracker, CallbackInfo ci) {
        final ClientComputeCameraRollEvent event = new ClientComputeCameraRollEvent(minecraft, minecraft.getCameraEntity(), deltaTracker.getGameTimeDeltaPartialTick(true));
        KnightLibEvents.CLIENT.dispatch(event);
        knightlib$cameraRoll = event.getRoll();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void knightlib$skipBobViewOnCameraPath(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (CameraPathManager.isPlaying() || CameraPathEditor.isActive()) {
            ci.cancel();
        }

    }

    @Inject(method = "bobHurt", at = @At("RETURN"))
    private void knightlib$applyCameraRoll(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (knightlib$cameraRoll != 0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(knightlib$cameraRoll));
        }

    }

    /**
     * Captures the projection handed to the level renderer since by the end of renderLevel the RenderSystem projection belongs to the hand pass
     */
    @ModifyArg(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"
            ),
            index = 6, require = 0
    )
    private Matrix4f knightlib$captureLevelProjection(Matrix4f projection) {
        knightlib$levelProjection.set(projection);
        knightlib$levelProjectionCaptured = true;
        return projection;
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void knightlib$afterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);

        final Matrix4f projection = knightlib$levelProjectionCaptured ? new Matrix4f(knightlib$levelProjection)
                : new Matrix4f(minecraft.gameRenderer.getProjectionMatrix(getFov(mainCamera, partialTicks, true)));
        knightlib$levelProjectionCaptured = false;

        final Matrix4f modelView = new Matrix4f().rotation(mainCamera.rotation().conjugate(new Quaternionf()));
        final Vec3 cameraPosition = mainCamera.getPosition();
        final PostShaderRenderContext context = new PostShaderRenderContext(
                PostShaderRenderStage.GAME_RENDERER_TAIL, partialTicks, projection, modelView, mainCamera, cameraPosition
        );

        PostShaderManager.renderStage(context);
    }

    /**
     * Runs right after the gui passes so the camera path is drawn anyway
     */
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    private void knightlib$renderCameraPathOverlay(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (renderLevel && minecraft.level != null) {
            final GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            CameraPathManager.renderOverlay(graphics, deltaTracker.getGameTimeDeltaPartialTick(true));
            graphics.flush();
        }

    }

}