package dev.xylonity.knightlib.client.armor;

import dev.xylonity.knightlib.api.client.armor.KnightLibArmorRenderer;
import dev.xylonity.knightlib.api.item.KnightLibRenderedArmorItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Forge renderer cache shared by its armor-model and armor-texture hooks
 */
public final class KnightLibForgeArmorRenderers {

    private static final Map<Item, KnightLibArmorRenderer> RENDERERS = new IdentityHashMap<>();

    private KnightLibForgeArmorRenderers() {
        ;;
    }

    public static KnightLibArmorRenderer get(Item item, KnightLibRenderedArmorItem renderedArmorItem) {
        synchronized (RENDERERS) {
            return RENDERERS.computeIfAbsent(item, ignored -> create(item, renderedArmorItem));
        }

    }

    private static KnightLibArmorRenderer create(Item item, KnightLibRenderedArmorItem renderedArmorItem) {
        final Object renderFactory = renderedArmorItem.armorRendererFactory().get();
        if (renderFactory instanceof KnightLibArmorRenderer renderer) {
            return renderer;
        }

        String type = renderFactory == null ? "null" : renderFactory.getClass().getName();
        throw new IllegalStateException("Armor renderer factory for " + BuiltInRegistries.ITEM.getKey(item) + " returned " + type + " instead of a KnightLibArmorRenderer");
    }

}
