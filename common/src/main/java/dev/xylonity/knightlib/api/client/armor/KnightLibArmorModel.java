package dev.xylonity.knightlib.api.client.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vanilla humanoid armor model with slot-specific part visibility
 */
public abstract class KnightLibArmorModel extends HumanoidModel<LivingEntity> {

    protected KnightLibArmorModel(ModelPart root) {
        super(root);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void copyPropertiesFrom(HumanoidModel<?> source) {
        ((HumanoidModel) source).copyPropertiesTo(this);
    }

    public abstract void prepareForSlot(EquipmentSlot slot);

}
