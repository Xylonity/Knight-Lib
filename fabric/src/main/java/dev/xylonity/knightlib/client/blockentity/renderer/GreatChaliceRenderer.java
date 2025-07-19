package dev.xylonity.knightlib.client.blockentity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.client.blockentity.model.GreatChaliceModel;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GreatChaliceRenderer extends GeoBlockRenderer<GreatChaliceBlockEntity> {

    public GreatChaliceRenderer(BlockEntityRendererProvider.Context rendererDispatcher) {
        super(new GreatChaliceModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, GreatChaliceBlockEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (bone.getName().equals("liquid")) {
            if (animatable.getCharges() == 0) return;

            poseStack.pushPose();

            if (animatable.getCharges() > 0) {
                float yOffset = ((animatable.getCharges() / 12f) * 8f) / 16f;
                poseStack.translate(bone.getPosX(),  yOffset, bone.getPosZ());
            }

            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

            poseStack.popPose();
            return;
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

}