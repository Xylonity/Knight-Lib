package dev.xylonity.knightlib.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.client.armor.KnightLibArmorModel;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public final class KnightLibFabricArmorRenderer implements ArmorRenderer {

    private final ModelLayerLocation layer;
    private final Function<ModelPart, KnightLibArmorModel> modelFactory;
    private final ResourceLocation texture;

    private KnightLibArmorModel model;

    public KnightLibFabricArmorRenderer(ModelLayerLocation layer, Function<ModelPart, KnightLibArmorModel> modelFactory, ResourceLocation texture) {
        this.layer = layer;
        this.modelFactory = modelFactory;
        this.texture = texture;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel) {
        if (this.model == null) {
            final ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(this.layer);
            this.model = this.modelFactory.apply(root);
        }

        this.model.copyPropertiesFrom(contextModel);
        this.model.prepareForSlot(slot);
        ArmorRenderer.renderPart(poseStack, buffers, light, stack, this.model, this.texture);
    }

}