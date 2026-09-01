package dev.xylonity.knightlib.api.item;

import java.util.function.Supplier;

/**
 * Common-side capability for vanilla {@code Item} subclasses that provide a custom KnightLib item renderer.
 */
public interface KnightLibRenderedItem extends KnightLibAnimatedItem {

    /**
     * Returns a factory for this item's client renderer. Implementations must return a synthetic reference,
     * such as () -> new CustomRenderer(name); to avoid client-sided references in the server
     */
    Supplier<Object> rendererFactory();

}