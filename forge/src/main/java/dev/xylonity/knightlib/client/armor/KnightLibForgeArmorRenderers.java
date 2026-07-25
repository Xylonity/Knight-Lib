package dev.xylonity.knightlib.client.armor;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.client.armor.KnightLibArmorRenderer;
import dev.xylonity.knightlib.api.item.KnightLibRenderedArmorItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Forge renderer cache shared by its armor-model and armor-texture hooks
 */
public final class KnightLibForgeArmorRenderers {

    private static final Map<Item, KnightLibArmorRenderer> RENDERERS = new IdentityHashMap<>();
    private static final Set<Item> MISSING_TEXTURE_WARNED = Collections.newSetFromMap(new IdentityHashMap<>());

    private KnightLibForgeArmorRenderers() {
        ;;
    }

    /**
     * Instantiates and checks every registered armor renderer during client setup, so a faulty factory fails there instead of later on
     */
    public static void bootstrap() {
        for (final Item item : BuiltInRegistries.ITEM) {
            if (item instanceof final KnightLibRenderedArmorItem renderedArmorItem) {
                get(item, renderedArmorItem);
            }

        }

    }

    public static KnightLibArmorRenderer get(Item item, KnightLibRenderedArmorItem renderedArmorItem) {
        synchronized (RENDERERS) {
            return RENDERERS.computeIfAbsent(item, ignored -> create(item, renderedArmorItem));
        }

    }

    /**
     * Reports a renderer that supplied no texture for an equipped stack, falling back to the default vanilla path
     */
    public static void warnMissingTexture(Item item) {
        synchronized (MISSING_TEXTURE_WARNED) {
            if (!MISSING_TEXTURE_WARNED.add(item)) {
                return;
            }

        }

        KnightLib.LOGGER.warn(
                "Armor renderer for {} returned no texture. Falling back to the " + " vanilla armor texture.", BuiltInRegistries.ITEM.getKey(item)
        );

    }

    private static KnightLibArmorRenderer create(Item item, KnightLibRenderedArmorItem renderedArmorItem) {
        final Object renderFactory = renderedArmorItem.armorRendererFactory().get();
        if (renderFactory instanceof KnightLibArmorRenderer renderer) {
            return renderer;
        }

        final String type = renderFactory == null ? "null" : renderFactory.getClass().getName();
        throw new IllegalStateException("Armor renderer factory for " + BuiltInRegistries.ITEM.getKey(item) + " returned " + type + " instead of a KnightLibArmorRenderer");
    }

}