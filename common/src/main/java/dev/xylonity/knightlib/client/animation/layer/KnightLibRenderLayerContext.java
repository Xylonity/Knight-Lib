package dev.xylonity.knightlib.client.animation.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import dev.xylonity.knightlib.client.animation.renderer.KnightLibItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Common vanilla render state supplied to every {@link KnightLibRenderLayer}.
 */
public final class KnightLibRenderLayerContext<T> {

    private final T target;
    private final KnightLibModel model;
    private final PoseStack poseStack;
    private final MultiBufferSource buffers;
    private final int packedLight;
    private final int packedOverlay;
    private final int renderColor;
    private final float partialTick;
    private final double renderTime;

    @Nullable
    private final ItemDisplayContext itemDisplayContext;
    private final boolean livingModelFrame;

    public KnightLibRenderLayerContext(T target, KnightLibModel model, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay, int renderColor, float partialTick, double renderTime, @Nullable ItemDisplayContext itemDisplayContext, boolean livingModelFrame) {
        this.target = Objects.requireNonNull(target, "target");
        this.model = Objects.requireNonNull(model, "model");
        this.poseStack = Objects.requireNonNull(poseStack, "poseStack");
        this.buffers = Objects.requireNonNull(buffers, "buffers");
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
        this.renderColor = renderColor;
        this.partialTick = partialTick;
        this.renderTime = renderTime;
        this.itemDisplayContext = itemDisplayContext;
        this.livingModelFrame = livingModelFrame;
    }

    public T target() {
        return target;
    }

    /**
     * The evaluated model and bone pose used by the base pass
     */
    public KnightLibModel model() {
        return model;
    }

    /**
     * Mutable vanilla pose stack for this pass. Every layer is isolated internally, so transforms applied
     * while rendering cannot affect the next layer.
     */
    public PoseStack poseStack() {
        return poseStack;
    }

    public MultiBufferSource buffers() {
        return buffers;
    }

    public VertexConsumer buffer(RenderType renderType) {
        return buffers.getBuffer(Objects.requireNonNull(renderType, "renderType"));
    }

    public int packedLight() {
        return packedLight;
    }

    public int packedOverlay() {
        return packedOverlay;
    }

    /**
     * Packed ARGB multiplier selected by the owning renderer for this complete render
     */
    public int renderColor() {
        return renderColor;
    }

    public KnightLibColor color() {
        return KnightLibColor.fromArgb(renderColor);
    }

    public float partialTick() {
        return partialTick;
    }

    /**
     * World render time in ticks, including {@link #partialTick()}
     */
    public double renderTime() {
        return renderTime;
    }

    /**
     * Non-null only when this pass belongs to a {@link KnightLibItemRenderer}
     */
    @Nullable
    public ItemDisplayContext itemDisplayContext() {
        return itemDisplayContext;
    }

    /**
     * Combines a layer tint with the renderer-wide ARGB
     */
    public int multiplyColor(int tintArgb) {
        return KnightLibColor.multiplyArgb(renderColor, tintArgb);
    }

    public KnightLibColor multiplyColor(@Nullable KnightLibColor tint) {
        return color().multiply(tint == null ? KnightLibColor.fromArgb(KnightLibColor.WHITE_ARGB) : tint);
    }

    /**
     * Re-renders the evaluated model with the renderer-wide ARGB and normal lighting
     */
    public void renderModel(RenderType renderType) {
        renderModel(renderType, packedLight, packedOverlay, renderColor);
    }

    /**
     * Re-renders the evaluated model with an exact packed ARGB and normal lighting
     */
    public void renderModel(RenderType renderType, int argb) {
        renderModel(renderType, packedLight, packedOverlay, argb);
    }

    public void renderModel(RenderType renderType, int light, int overlay, int argb) {
        renderModel(buffer(renderType), light, overlay, argb);
    }

    /**
     * Re-renders the evaluated model through an already decorated consumer, useful for foil or atlas-sprite wrappers for example
     */
    public void renderModel(VertexConsumer consumer, int light, int overlay, int argb) {
        Objects.requireNonNull(consumer, "consumer");
        final KnightLibColor resolved = KnightLibColor.fromArgb(argb);
        if (livingModelFrame) {
            model.renderLiving(poseStack, consumer, light, overlay, resolved.red(), resolved.green(), resolved.blue(), resolved.alpha());
        }
        else {
            model.render(poseStack, consumer, light, overlay, resolved.red(), resolved.green(), resolved.blue(), resolved.alpha());
        }

    }

    /**
     * Visits all bones in the frame used by this layer
     */
    public void visitBones(KnightLibModel.BoneVisitor visitor) {
        if (livingModelFrame) {
            model.visitLivingBones(poseStack, Objects.requireNonNull(visitor, "visitor"));
        }
        else {
            model.visitBones(poseStack, Objects.requireNonNull(visitor, "visitor"));
        }

    }

    public void visitBones(Set<String> boneNames, KnightLibModel.BoneVisitor visitor) {
        Objects.requireNonNull(boneNames, "boneNames");
        Objects.requireNonNull(visitor, "visitor");
        if (livingModelFrame) {
            model.visitLivingBones(poseStack, boneNames, visitor);
        }
        else {
            model.visitBones(poseStack, boneNames, visitor);
        }

    }

    /**
     * Iterates authored bone names in parent-first order when the model exposes a hierarchy
     */
    public void forEachBone(Consumer<String> visitor) {
        model.forEachBone(Objects.requireNonNull(visitor, "visitor"));
    }

}