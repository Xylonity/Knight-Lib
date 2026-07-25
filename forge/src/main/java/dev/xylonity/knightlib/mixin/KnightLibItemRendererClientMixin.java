package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.client.armor.KnightLibArmorRenderer;
import dev.xylonity.knightlib.api.item.KnightLibRenderedArmorItem;
import dev.xylonity.knightlib.api.item.KnightLibRenderedItem;
import dev.xylonity.knightlib.client.armor.KnightLibForgeArmorRenderers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Forge hook for vanilla item subclasses that have a custom stack renderer
 */
@Mixin(Item.class)
public abstract class KnightLibItemRendererClientMixin {

    @Inject(
            method = "initializeClient",
            at = @At("HEAD"),
            remap = false
    )
    private void knightlib$initializeItemRenderer(Consumer<IClientItemExtensions> consumer, CallbackInfo ci) {
        final Item item = (Item) (Object) this;
        final KnightLibRenderedItem renderedItem = item instanceof KnightLibRenderedItem knightLibRenderedItem ? knightLibRenderedItem : null;
        final KnightLibRenderedArmorItem renderedArmorItem = item instanceof KnightLibRenderedArmorItem knightLibRenderedArmorItem ? knightLibRenderedArmorItem : null;
        if (renderedItem == null && renderedArmorItem == null) {
            return;
        }

        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderedItem == null) {
                    return IClientItemExtensions.super.getCustomRenderer();
                }
                if (renderer == null) {
                    final Object renderFactory = renderedItem.rendererFactory().get();
                    if (!(renderFactory instanceof BlockEntityWithoutLevelRenderer itemRenderer)) {
                        final String type = renderFactory == null ? "null" : renderFactory.getClass().getName();
                        throw new IllegalStateException("Renderer factory for " + BuiltInRegistries.ITEM.getKey(item) + " returned " + type + " instead of a BlockEntityWithoutLevelRenderer");
                    }

                    renderer = itemRenderer;
                }

                return renderer;
            }

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (renderedArmorItem == null) {
                    return original;
                }

                final KnightLibArmorRenderer armorRenderer = KnightLibForgeArmorRenderers.get(item, renderedArmorItem);
                return armorRenderer.prepareModel(entity, stack, slot, original);
            }

        });

    }

}