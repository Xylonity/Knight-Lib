package dev.xylonity.knightlib.client.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.animation.KnightLibAnimationSource;
import dev.xylonity.knightlib.client.animation.KnightLibAnimator;
import dev.xylonity.knightlib.client.animation.KnightLibModelSource;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Vanilla {@link EntityModel} bridge backed by a dynamically selected {@link KnightLibModel}.
 */
public final class KnightLibLivingModel<T extends LivingEntity & KnightLibAnimatable> extends EntityModel<T> implements ArmedModel, HeadedModel {

    private final KnightLibModelSource.InstanceCache modelCache = new KnightLibModelSource.InstanceCache();

    private Function<T, KnightLibModelSource> modelSource;
    private Function<T, KnightLibAnimationSource> animationSource;
    private PoseSetup<T> poseSetup;
    private RenderColorProvider<T> renderColorProvider = (entity, partialTick) -> KnightLibColor.fromArgb(KnightLibColor.WHITE_ARGB);
    private KnightLibModel activeModel;
    private int activeRenderColor = KnightLibColor.WHITE_ARGB;
    private float partialTick;
    private T activeEntity;
    private boolean attachmentsPrepared;

    private Function<T, String> headBone = entity -> "head";
    private BiFunction<T, HumanoidArm, String> armBone = (entity, arm) -> arm == HumanoidArm.LEFT ? "left_arm" : "right_arm";
    private final ModelPart headAttachment = new ModelPart(List.of(), Map.of());
    private final AttachmentTransform leftHand = new AttachmentTransform();
    private final AttachmentTransform rightHand = new AttachmentTransform();

    public KnightLibLivingModel() {
        super(RenderType::entityCutoutNoCull);
    }

    public void configure(Function<T, KnightLibModelSource> modelSource, Function<T, KnightLibAnimationSource> animationSource, PoseSetup<T> poseSetup) {
        this.modelSource = Objects.requireNonNull(modelSource, "modelSource");
        this.animationSource = Objects.requireNonNull(animationSource, "animationSource");
        this.poseSetup = Objects.requireNonNull(poseSetup, "poseSetup");
    }

    public void configureAttachments(Function<T, String> headBone, BiFunction<T, HumanoidArm, String> armBone) {
        this.headBone = Objects.requireNonNull(headBone, "headBone");
        this.armBone = Objects.requireNonNull(armBone, "armBone");
    }

    public void configureRenderColor(RenderColorProvider<T> renderColorProvider) {
        this.renderColorProvider = Objects.requireNonNull(renderColorProvider, "renderColorProvider");
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (modelSource == null) {
            return;
        }

        activeModel = modelCache.resolve(modelSource.apply(entity));

        final KnightLibAnimationSource animations = animationSource.apply(entity);
        final double now = entity.level().getGameTime() + partialTick;
        KnightLibAnimator.animate(entity.getAnimationHandler(), activeModel, animations::get, now);

        poseSetup.setup(entity, activeModel, limbSwing, limbSwingAmount, partialTick, netHeadYaw, headPitch);

        final KnightLibColor color = renderColorProvider.get(entity, partialTick);
        activeRenderColor = color == null ? KnightLibColor.WHITE_ARGB : color.toArgb();
        activeEntity = entity;

        attachmentsPrepared = false;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (activeModel != null) {
            final KnightLibColor color = KnightLibColor.fromArgb(activeRenderColor);
            activeModel.renderLiving(poseStack, consumer, packedLight, packedOverlay,
                    red * color.red(), green * color.green(),
                    blue * color.blue(), alpha * color.alpha());
        }

    }

    public KnightLibModel activeModel() {
        return activeModel;
    }

    public KnightLibColor renderColor() {
        return KnightLibColor.fromArgb(activeRenderColor);
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        prepareAttachments();
        (arm == HumanoidArm.LEFT ? leftHand : rightHand).apply(poseStack);
    }

    @Override
    public ModelPart getHead() {
        prepareAttachments();
        return headAttachment;
    }

    /**
     * Applies the current transform of an arbitrary bone for custom vanilla render layers
     */
    public boolean translateToBone(String bone, PoseStack target) {
        if (activeModel == null || bone == null || !activeModel.hasBone(bone)) {
            return false;
        }

        final AttachmentTransform transform = new AttachmentTransform();
        final PoseStack identity = new PoseStack();
        activeModel.visitLivingBones(identity, Set.of(bone), (name, pose, normal) -> transform.set(pose, normal));

        transform.apply(target);

        return transform.valid;
    }

    private void prepareAttachments() {
        if (attachmentsPrepared) {
            return;
        }

        attachmentsPrepared = true;
        leftHand.reset();
        rightHand.reset();
        resetHeadAttachment();

        final T entity = activeEntity;
        if (entity == null || activeModel == null) {
            return;
        }

        final String head = headBone.apply(entity);
        final String left = armBone.apply(entity, HumanoidArm.LEFT);
        final String right = armBone.apply(entity, HumanoidArm.RIGHT);
        final Set<String> names = new HashSet<>();
        if (head != null && activeModel.hasBone(head)) {
            names.add(head);
        }
        if (left != null && activeModel.hasBone(left)) {
            names.add(left);
        }
        if (right != null && activeModel.hasBone(right)) {
            names.add(right);
        }
        if (names.isEmpty()) {
            return;
        }

        activeModel.visitLivingBones(new PoseStack(), names, (name, pose, normal) -> {
            if (name.equals(head)) {
                decomposeHead(pose);
            }
            if (name.equals(left)) {
                leftHand.set(pose, normal);
            }
            if (name.equals(right)) {
                rightHand.set(pose, normal);
            }

        });

    }

    private void resetHeadAttachment() {
        headAttachment.setPos(0f, 0f, 0f);
        headAttachment.setRotation(0f, 0f, 0f);
        headAttachment.xScale = 1f;
        headAttachment.yScale = 1f;
        headAttachment.zScale = 1f;
    }

    private void decomposeHead(Matrix4f pose) {
        final Vector3f translation = pose.getTranslation(new Vector3f());
        final Matrix3f rotation = new Matrix3f(pose);
        float scaleX = columnLength(rotation, 0);
        final float scaleY = columnLength(rotation, 1);
        final float scaleZ = columnLength(rotation, 2);
        if (!finitePositive(scaleX) || !finitePositive(scaleY) || !finitePositive(scaleZ)) {
            return;
        }
        if (rotation.determinant() < 0f) {
            scaleX = -scaleX;
        }

        normalizeColumns(rotation, scaleX, scaleY, scaleZ);

        final Vector3f angles = rotation.getEulerAnglesZYX(new Vector3f());
        if (!Float.isFinite(angles.x()) || !Float.isFinite(angles.y()) || !Float.isFinite(angles.z())) {
            return;
        }

        headAttachment.setPos(translation.x() * 16f, translation.y() * 16f, translation.z() * 16f);
        headAttachment.setRotation(angles.x(), angles.y(), angles.z());
        headAttachment.xScale = scaleX;
        headAttachment.yScale = scaleY;
        headAttachment.zScale = scaleZ;
    }

    private static boolean finitePositive(float value) {
        return Float.isFinite(value) && Math.abs(value) > 1.0E-6f;
    }

    private static float columnLength(Matrix3f matrix, int column) {
        return switch (column) {
            case 0 -> (float) Math.sqrt(matrix.m00 * matrix.m00 + matrix.m01 * matrix.m01 + matrix.m02 * matrix.m02);
            case 1 -> (float) Math.sqrt(matrix.m10 * matrix.m10 + matrix.m11 * matrix.m11 + matrix.m12 * matrix.m12);
            case 2 -> (float) Math.sqrt(matrix.m20 * matrix.m20 + matrix.m21 * matrix.m21 + matrix.m22 * matrix.m22);
            default -> throw new IllegalArgumentException("[KnightLib] column must be 0..2");
        };

    }

    private static void normalizeColumns(Matrix3f matrix, float x, float y, float z) {
        matrix.m00 /= x;
        matrix.m01 /= x;
        matrix.m02 /= x;
        matrix.m10 /= y;
        matrix.m11 /= y;
        matrix.m12 /= y;
        matrix.m20 /= z;
        matrix.m21 /= z;
        matrix.m22 /= z;
    }

    private static final class AttachmentTransform {

        private final Matrix4f pose = new Matrix4f();
        private final Matrix3f normal = new Matrix3f();
        private boolean valid;

        void set(Matrix4f pose, Matrix3f normal) {
            this.pose.set(pose);
            this.normal.set(normal);
            this.valid = true;
        }

        void reset() {
            this.pose.identity();
            this.normal.identity();
            this.valid = false;
        }

        void apply(PoseStack target) {
            if (!valid) {
                return;
            }

            target.mulPoseMatrix(pose);
            target.last().normal().mul(normal);
        }

    }

    @FunctionalInterface
    public interface PoseSetup<T> {

        void setup(T entity, KnightLibModel model, float limbSwing, float limbSwingAmount, float partialTick, float netHeadYaw, float headPitch);

    }

    @FunctionalInterface
    public interface RenderColorProvider<T> {

        KnightLibColor get(T entity, float partialTick);

    }

}