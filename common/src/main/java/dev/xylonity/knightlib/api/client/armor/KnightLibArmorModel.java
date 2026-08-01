package dev.xylonity.knightlib.api.client.armor;

import dev.xylonity.knightlib.client.animation.KnightLibPose;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import dev.xylonity.knightlib.client.animation.model.VanillaModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vanilla humanoid armor model with slot-specific part visibility
 */
public abstract class KnightLibArmorModel extends HumanoidModel<LivingEntity> {

    private final KnightLibModel animationModel;

    protected KnightLibArmorModel(ModelPart root) {
        this(root, new VanillaModel(root));
    }

    protected KnightLibArmorModel(ModelPart root, KnightLibModel animationModel) {
        super(root);
        this.animationModel = animationModel;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void copyPropertiesFrom(HumanoidModel<?> source) {
        ((HumanoidModel) source).copyPropertiesTo(this);
    }

    /**
     * Mutable model driven by KnightLib's animation controllers
     */
    public final KnightLibModel animationModel() {
        return animationModel;
    }

    /**
     * Composes the current animation over the current pose supplied by vanilla
     */
    public void composeWithWearer(HumanoidModel<?> source, KnightLibPose animatedPose, KnightLibPose restPose) {
        animationModel.resetPose();
        copyPropertiesFrom(source);
        animationModel.applyPoseDelta(animatedPose, restPose);
    }

    /**
     * Shows only the parts that belong to {@code slot}, matching vanilla's own armor visibility
     */
    public void prepareForSlot(EquipmentSlot slot) {
        setAllVisible(false);

        switch (slot) {
            case HEAD -> {
                head.visible = true;
                hat.visible = true;
            }
            case CHEST -> {
                body.visible = true;
                rightArm.visible = true;
                leftArm.visible = true;
            }
            case LEGS -> {
                body.visible = true;
                rightLeg.visible = true;
                leftLeg.visible = true;
            }
            case FEET -> {
                rightLeg.visible = true;
                leftLeg.visible = true;
            }
            default -> {
                ;;
            }

        }

    }

}