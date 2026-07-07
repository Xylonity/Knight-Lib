package dev.xylonity.knightlib.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathManager;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Final
    @Shadow
    Minecraft minecraft;

    @Final
    @Shadow
    private Camera mainCamera;

    @Inject(
            method = "renderLevel",
            at = @At("TAIL")
    )
    private void knightlib$afterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);

        Matrix4f projection = new Matrix4f(minecraft.gameRenderer.getProjectionMatrix(partialTicks));
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());

        Vec3 cameraPosition = mainCamera.getPosition();

        PostShaderRenderContext context = new PostShaderRenderContext(
                PostShaderRenderStage.GAME_RENDERER_TAIL,
                partialTicks,
                projection,
                modelView,
                mainCamera,
                cameraPosition
        );

        PostShaderManager.renderStage(context);
    }

    /**
     * Runs right after the gui passes so the camera path is drawn anyway
     */
    @Inject(
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V")
    )
    private void knightlib$renderCameraPathOverlay(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (renderLevel && minecraft.level != null) {
            final GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            CameraPathManager.renderOverlay(graphics, deltaTracker.getGameTimeDeltaPartialTick(true));
            graphics.flush();
        }

    }

}
