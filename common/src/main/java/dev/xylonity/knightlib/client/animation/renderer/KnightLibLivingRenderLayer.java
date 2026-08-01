package dev.xylonity.knightlib.client.animation.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayer;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayerContext;
import dev.xylonity.knightlib.client.animation.model.KnightLibLivingModel;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Render layer for a trivial layer within the vanilla's rendering pipeline
 */
final class KnightLibLivingRenderLayer<T extends LivingEntity & KnightLibAnimatable> extends RenderLayer<T, KnightLibLivingModel<T>> {

    private final KnightLibRenderLayer<T> layer;

    public KnightLibLivingRenderLayer(RenderLayerParent<T, KnightLibLivingModel<T>> parent, KnightLibRenderLayer<T> layer) {
        super(parent);
        this.layer = layer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        final KnightLibLivingModel<T> parentModel = getParentModel();
        final KnightLibModel model = parentModel.activeModel();
        if (model == null) {
            return;
        }

        final int packedOverlay = LivingEntityRenderer.getOverlayCoords(entity, 0f);
        final double renderTime = entity.level().getGameTime() + partialTick;
        final KnightLibRenderLayerContext<T> context = new KnightLibRenderLayerContext<>(
                entity, model, poseStack, buffers, packedLight, packedOverlay,
                parentModel.renderColor().toArgb(), partialTick, renderTime, null, true
        );

        layer.renderIsolated(context);
    }

}