package dev.xylonity.knightlib.api.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.client.animation.KnightLibPose;
import dev.xylonity.knightlib.client.animation.model.GeoModel;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Humanoud armor model with a {@link KnightLibModel} underneath that lets vanilla draw geo geometry.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/renderer/GeoArmorRenderer.java
 */
public final class KnightLibGeoArmorModel extends KnightLibArmorModel {

    private final GeoModel geoModel;
    private final BoneNames bones;

    private final PartRestPose headRest;
    private final PartRestPose bodyRest;
    private final PartRestPose rightArmRest;
    private final PartRestPose leftArmRest;
    private final PartRestPose rightLegRest;
    private final PartRestPose leftLegRest;

    private EquipmentSlot activeSlot;

    KnightLibGeoArmorModel(ModelPart root, GeoModel geoModel, BoneNames bones) {
        super(root, geoModel);
        this.geoModel = geoModel;
        this.bones = bones;

        this.headRest = PartRestPose.capture(head);
        this.bodyRest = PartRestPose.capture(body);
        this.rightArmRest = PartRestPose.capture(rightArm);
        this.leftArmRest = PartRestPose.capture(leftArm);
        this.rightLegRest = PartRestPose.capture(rightLeg);
        this.leftLegRest = PartRestPose.capture(leftLeg);

        if (bones.all().stream().noneMatch(geoModel::hasBone)) {
            throw new IllegalArgumentException("[KnightLib] Geo armor model has none of its configured humanoid bones: " + bones.all());
        }

    }

    public GeoModel geoModel() {
        return geoModel;
    }

    @Override
    public void composeWithWearer(HumanoidModel<?> source, KnightLibPose animatedPose, KnightLibPose restPose) {
        // The humanoid parts carry vanilla's movement pose
        geoModel.resetPose();
        geoModel.applyPose(animatedPose);
        copyPropertiesFrom(source);

        applyPart(bones.head(), head, headRest);
        applyPart(bones.body(), body, bodyRest);
        applyPart(bones.leggingsBody(), body, bodyRest);
        applyPart(bones.rightArm(), rightArm, rightArmRest);
        applyPart(bones.leftArm(), leftArm, leftArmRest);
        applyPart(bones.rightLeg(), rightLeg, rightLegRest);
        applyPart(bones.leftLeg(), leftLeg, leftLegRest);
        applyPart(bones.rightBoot(), rightLeg, rightLegRest);
        applyPart(bones.leftBoot(), leftLeg, leftLegRest);
    }

    private void applyPart(@Nullable String boneName, ModelPart part, PartRestPose restPose) {
        if (boneName == null || !geoModel.hasBone(boneName)) {
            return;
        }

        geoModel.applyPosition(boneName,
                part.x - restPose.x(),
                restPose.y() - part.y,
                part.z - restPose.z()
        );
        geoModel.applyRotation(boneName,
                (float) Math.toDegrees(part.xRot - restPose.xRot()),
                (float) Math.toDegrees(part.yRot - restPose.yRot()),
                (float) Math.toDegrees(part.zRot - restPose.zRot())
        );
        geoModel.applyScale(boneName,
                scaleRatio(part.xScale, restPose.xScale()),
                scaleRatio(part.yScale, restPose.yScale()),
                scaleRatio(part.zScale, restPose.zScale())
        );

    }

    private static float scaleRatio(float value, float rest) {
        return Math.abs(rest) < 1.0E-6f ? 1f : value / rest;
    }

    @Override
    public void prepareForSlot(EquipmentSlot slot) {
        activeSlot = slot;
        setMappedVisible(false);

        switch (slot) {
            case HEAD -> setVisible(bones.head(), true);
            case CHEST -> {
                setVisible(bones.body(), true);
                setVisible(bones.rightArm(), true);
                setVisible(bones.leftArm(), true);
            }
            case LEGS -> {
                setVisible(bones.leggingsBody(), true);
                setVisible(bones.rightLeg(), true);
                setVisible(bones.leftLeg(), true);
            }
            case FEET -> {
                setVisible(firstPresent(bones.rightBoot(), bones.rightLeg()), true);
                setVisible(firstPresent(bones.leftBoot(), bones.leftLeg()), true);
            }
            default -> {
                ;;
            }

        }

    }

    private void setMappedVisible(boolean visible) {
        for (final String bone : bones.all()) {
            setVisible(bone, visible);
        }

    }

    private void setVisible(@Nullable String bone, boolean visible) {
        if (bone != null && geoModel.hasBone(bone)) {
            geoModel.setBoneVisible(bone, visible);
        }

    }

    private @Nullable String firstPresent(@Nullable String preferred, @Nullable String fallback) {
        return preferred != null && geoModel.hasBone(preferred) ? preferred : fallback;
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (activeSlot == null) {
            return;
        }

        poseStack.pushPose();

        if (young) {
            if (activeSlot == EquipmentSlot.HEAD) {
                poseStack.scale(0.75f, 0.75f, 0.75f);
                poseStack.translate(0f, 1f, 0f);
            }
            else {
                poseStack.scale(0.5f, 0.5f, 0.5f);
                poseStack.translate(0f, 1.5f, 0f);
            }

        }

        applyArmorTransform(poseStack);
        geoModel.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.popPose();
    }

    static void applyArmorTransform(PoseStack poseStack) {
        poseStack.translate(0f, 24f / 16f, 0f);
        poseStack.scale(-1f, -1f, 1f);
    }

    record BoneNames(
            @Nullable String head,
            @Nullable String body,
            @Nullable String leggingsBody,
            @Nullable String rightArm,
            @Nullable String leftArm,
            @Nullable String rightLeg,
            @Nullable String leftLeg,
            @Nullable String rightBoot,
            @Nullable String leftBoot
    ) {

        Set<String> all() {
            final Set<String> names = new LinkedHashSet<>();
            add(names, head);
            add(names, body);
            add(names, leggingsBody);
            add(names, rightArm);
            add(names, leftArm);
            add(names, rightLeg);
            add(names, leftLeg);
            add(names, rightBoot);
            add(names, leftBoot);
            return names;
        }

        private static void add(Set<String> names, @Nullable String name) {
            if (name != null && !name.isBlank()) {
                names.add(name);
            }

        }

    }

    private record PartRestPose(
            float x,
            float y,
            float z,
            float xRot,
            float yRot,
            float zRot,
            float xScale,
            float yScale,
            float zScale
    ) {

        static PartRestPose capture(ModelPart part) {
            return new PartRestPose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale);
        }

    }

}