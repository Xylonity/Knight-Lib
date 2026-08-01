package dev.xylonity.knightlib.api.item;

import java.util.function.Supplier;

/**
 * Common-side capability for vanilla {@code ArmorItem} subclasses that replace the model used
 * while the stack is equipped
 */
public interface KnightLibRenderedArmorItem extends KnightLibAnimatedItem {

    /**
     * Returns a factory for this equipped's armor renderer. Implementations must return a synthetic reference,
     * such as () -> new CustomArmorRenderer(name); to avoid client-sided references in the server
     */
    Supplier<Object> armorRendererFactory();

}
