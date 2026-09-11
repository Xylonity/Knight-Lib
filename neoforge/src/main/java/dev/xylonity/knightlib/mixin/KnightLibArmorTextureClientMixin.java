package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.client.armor.KnightLibArmorRenderer;
import dev.xylonity.knightlib.api.item.KnightLibRenderedArmorItem;
import dev.xylonity.knightlib.client.armor.KnightLibNeoForgeArmorRenderers;
import dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies the texture belonging to the same armor renderer used for NeoForge's model hook (not requiring the texture to be separated into different layers)
 */
@Mixin(value = ClientHooks.class, remap = false)
public abstract class KnightLibArmorTextureClientMixin {

    @Inject(
            method = "getArmorTexture",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void knightlib$getArmorTexture(Entity entity, ItemStack stack, ArmorMaterial.Layer layer, boolean innerModel, EquipmentSlot slot, CallbackInfoReturnable<ResourceLocation> cir) {
        final Item item = stack.getItem();
        if (!(entity instanceof LivingEntity owner) || !(item instanceof KnightLibRenderedArmorItem renderedArmorItem)) {
            return;
        }

        final KnightLibArmorRenderer renderer = KnightLibNeoForgeArmorRenderers.get(item, renderedArmorItem);
        final ResourceLocation texture = renderer.getTextureLocation(stack, owner, slot, null);
        if (texture == null) {
            KnightLibNeoForgeArmorRenderers.warnMissingTexture(item);
            return;
        }

        cir.setReturnValue(KnightLibAnimatedTextures.resolve(texture));
    }

}
