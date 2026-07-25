package dev.xylonity.knightlib.common.item.armor;

import dev.xylonity.knightlib.api.animation.KnightLibItemAnimations;
import dev.xylonity.knightlib.api.item.KnightLibRenderedArmorItem;
import dev.xylonity.knightlib.client.armor.GenericKnightLibArmorRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementation of a vanilla armor item backed by a registered KnightLib armor model.
 *
 * Geo models must override {@code armorRendererFactory} with a custom {@code dev.xylonity.knightlib.api.client.armor.KnightLibGeoArmorRenderer}
 * renderer, while vanilla models must register a custom model through {@code ArmorModelRegistrationEvent}
 */
public class GenericArmorItem extends ArmorItem implements KnightLibRenderedArmorItem {

    private final ResourceLocation modelId;

    public GenericArmorItem(ArmorMaterial material, Type type, Properties properties, ResourceLocation modelId) {
        super(material, type, properties);
        this.modelId = Objects.requireNonNull(modelId, "modelId");
    }

    public final ResourceLocation modelId() {
        return modelId;
    }

    /**
     * Stub that matches Forge's optional item hook
     */
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return KnightLibItemAnimations.shouldCauseReequipAnimation(oldStack, newStack);
    }

    @Override
    public Supplier<Object> armorRendererFactory() {
        return () -> new GenericKnightLibArmorRenderer(modelId);
    }

}
