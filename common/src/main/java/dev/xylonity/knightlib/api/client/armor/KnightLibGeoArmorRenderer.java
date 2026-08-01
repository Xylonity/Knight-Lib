package dev.xylonity.knightlib.api.client.armor;

import dev.xylonity.knightlib.client.animation.KnightLibAnimationAssets;
import dev.xylonity.knightlib.client.animation.model.GeoModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Equipped-armor renderer backed by a geo model
 */
public abstract class KnightLibGeoArmorRenderer extends KnightLibArmorRenderer {

    protected abstract ResourceLocation getGeoModelLocation();

    protected @Nullable String getHeadBone() {
        return "armorHead";
    }

    protected @Nullable String getBodyBone() {
        return "armorBody";
    }

    protected @Nullable String getLeggingsBodyBone() {
        return "armorLeggingsBody";
    }

    protected @Nullable String getRightArmBone() {
        return "armorRightArm";
    }

    protected @Nullable String getLeftArmBone() {
        return "armorLeftArm";
    }

    protected @Nullable String getRightLegBone() {
        return "armorRightLeg";
    }

    protected @Nullable String getLeftLegBone() {
        return "armorLeftLeg";
    }

    protected @Nullable String getRightBootBone() {
        return "armorRightBoot";
    }

    protected @Nullable String getLeftBootBone() {
        return "armorLeftBoot";
    }

    @Override
    protected final KnightLibArmorModel createModel() {
        final ResourceLocation location = Objects.requireNonNull(getGeoModelLocation(), "geo model location");
        final GeoModel geoModel = new GeoModel(KnightLibAnimationAssets.getModel(location));
        final ModelPart humanoidRoot = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR);
        return new KnightLibGeoArmorModel(humanoidRoot, geoModel,
                new KnightLibGeoArmorModel.BoneNames(
                        getHeadBone(), getBodyBone(), getLeggingsBodyBone(),
                        getRightArmBone(), getLeftArmBone(),
                        getRightLegBone(), getLeftLegBone(),
                        getRightBootBone(), getLeftBootBone()
                )

        );

    }

    @Override
    protected final int modelGeneration() {
        return KnightLibAnimationAssets.generation();
    }

}