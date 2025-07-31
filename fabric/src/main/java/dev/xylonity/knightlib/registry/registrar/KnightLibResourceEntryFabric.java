package dev.xylonity.knightlib.registry.registrar;

import net.minecraft.resources.ResourceLocation;

/**
 * Wraps a RegistryObject to implement ResourceEntry exposing both abstracts
 */
public class KnightLibResourceEntryFabric<T> implements ResourceEntry<T> {

    private final T entry;
    private final ResourceLocation id;

    public KnightLibResourceEntryFabric(T entry, ResourceLocation id) {
        this.entry = entry;
        this.id = id;
    }

    @Override
    public T get() {
        return entry;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

}