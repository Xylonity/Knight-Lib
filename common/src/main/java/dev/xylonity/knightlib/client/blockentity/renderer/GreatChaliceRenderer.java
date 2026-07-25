package dev.xylonity.knightlib.client.blockentity.renderer;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.client.animation.KnightLibAnimationSource;
import dev.xylonity.knightlib.client.animation.KnightLibModelSource;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import dev.xylonity.knightlib.client.animation.renderer.KnightLibBlockEntityRenderer;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class GreatChaliceRenderer extends KnightLibBlockEntityRenderer<GreatChaliceBlockEntity> {

    private static final ResourceLocation NORMAL_TEXTURE = KnightLib.of("textures/block/great_chalice.png");
    private static final ResourceLocation CHAOTIC_TEXTURE = KnightLib.of("textures/block/great_chalice_chaotic.png");

    public GreatChaliceRenderer(BlockEntityRendererProvider.Context context) {
        ;;
    }

    @Override
    protected KnightLibModelSource defineModel(GreatChaliceBlockEntity chalice) {
        return KnightLibModelSource.geo(KnightLib.of("geo/great_chalice.geo.json"));
    }

    @Override
    protected KnightLibAnimationSource defineAnimations(GreatChaliceBlockEntity chalice) {
        return KnightLibAnimationSource.geo(KnightLib.of("animations/great_chalice.animation.json"));
    }

    @Override
    public ResourceLocation getTextureLocation(GreatChaliceBlockEntity chalice) {
        return switch (chalice.getState()) {
            case CHAOTIC -> CHAOTIC_TEXTURE;
            default -> NORMAL_TEXTURE; // EMPTY/NORMAL
        };

    }

    @Override
    protected void setupPose(GreatChaliceBlockEntity chalice, KnightLibModel model, float partialTicks) {
        model.setBoneVisible("liquid", chalice.getCharges() > 0);

        if (chalice.getCharges() > 0) {
            // The liquid surface rises up to 8/16 of a block at max charges
            model.applyPosition("liquid", 0f, (chalice.getCharges() / 12f) * 8f, 0f);
        }

    }

}