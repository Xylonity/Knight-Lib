package dev.xylonity.knightlib.registry.registrar;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Wrapper that handles one entry in a ResourceRegistry.
 * Example usage:
 * <pre>
 * {@code
 * public static final ResourceEntry<Item> FILLED_GRAIL = ITEMS.register("filled_grail", () -> new FilledGrailItem(new Item.Properties()));
 * }
 * </pre>
 * @param <T> The type of the entry.
 */
public interface ResourceEntry<T> extends Supplier<T> {

    /**
     * Returns the main registered object (entity, item, block, etc.)
     */
    @Override
    T get();

    /**
     * Namespace and name of the object encapsulated
     */
    ResourceLocation getId();
}