package dev.xylonity.knightlib.common.item.armor;

import dev.xylonity.knightlib.api.client.armor.KnightLibArmorModel;
import dev.xylonity.knightlib.client.armor.KnightLibForgeArmorModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public final class KnightLibArmorItemForge extends ArmorItem {

    private final ResourceLocation modelId;

    public KnightLibArmorItemForge(ArmorMaterial material, Type type, Properties properties, ResourceLocation modelId) {
        super(material, type, properties);
        this.modelId = modelId;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private KnightLibArmorModel model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (this.model == null) {
                    final KnightLibForgeArmorModels.Definition definition = KnightLibForgeArmorModels.get(KnightLibArmorItemForge.this.modelId);
                    final ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(definition.layer());
                    this.model = definition.modelFactory().apply(root);
                }

                this.model.copyPropertiesFrom(original);
                this.model.prepareForSlot(slot);

                return this.model;
            }

        });

    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return KnightLibForgeArmorModels.get(this.modelId).texture().toString();
    }

}