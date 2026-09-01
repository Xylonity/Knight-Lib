package dev.xylonity.knightlib.client.armor;

import dev.xylonity.knightlib.api.client.armor.KnightLibArmorModel;
import dev.xylonity.knightlib.api.client.armor.KnightLibArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Default renderer for conventionally registered vanilla armor models.
 */
public class GenericKnightLibArmorRenderer extends KnightLibArmorRenderer {

    private final ResourceLocation modelId;

    public GenericKnightLibArmorRenderer(ResourceLocation modelId) {
        this.modelId = modelId;
    }

    @Override
    protected KnightLibArmorModel createModel() {
        final KnightLibArmorModels.Definition definition = KnightLibArmorModels.get(modelId);
        final ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(definition.layer());
        return definition.modelFactory().apply(root);
    }

    @Override
    public ResourceLocation getTextureLocation(ItemStack stack, LivingEntity wearer, EquipmentSlot slot, @Nullable String layer) {
        return KnightLibArmorModels.get(modelId).texture();
    }

}
