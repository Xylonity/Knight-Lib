package dev.xylonity.knightlib.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.client.armor.KnightLibArmorRenderer;
import dev.xylonity.knightlib.api.item.KnightLibRenderedArmorItem;
import dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class KnightLibFabricArmorRenderer implements ArmorRenderer {

    private final Item item;
    private final KnightLibRenderedArmorItem renderedArmorItem;

    private KnightLibArmorRenderer renderer;
    private boolean warnedMissingTexture;

    public KnightLibFabricArmorRenderer(Item item, KnightLibRenderedArmorItem renderedArmorItem) {
        this.item = item;
        this.renderedArmorItem = renderedArmorItem;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel) {
        final KnightLibArmorRenderer armorRenderer = renderer();
        final ResourceLocation texture = armorRenderer.getTextureLocation(stack, entity, slot, null);
        if (texture == null) {
            warnMissingTexture();
            return;
        }

        ArmorRenderer.renderPart(
                poseStack, buffers, light, stack,
                armorRenderer.prepareModel(entity, stack, slot, contextModel),
                KnightLibAnimatedTextures.resolve(texture)
        );

    }

    private void warnMissingTexture() {
        if (warnedMissingTexture) {
            return;
        }

        warnedMissingTexture = true;
        KnightLib.LOGGER.warn(
                "Armor renderer for {} returned no texture, so the equipped stack will " + "not be rendered", BuiltInRegistries.ITEM.getKey(item)
        );

    }

    private KnightLibArmorRenderer renderer() {
        if (renderer != null) {
            return renderer;
        }

        final Object armorRendererFactory = renderedArmorItem.armorRendererFactory().get();
        if (!(armorRendererFactory instanceof KnightLibArmorRenderer armorRenderer)) {
            final String type = armorRendererFactory == null ? "null" : armorRendererFactory.getClass().getName();
            throw new IllegalStateException("[KnightLib] Armor renderer factory for " + BuiltInRegistries.ITEM.getKey(item) + " returned " + type + " instead of a KnightLibArmorRenderer");
        }

        renderer = armorRenderer;
        return renderer;
    }

}