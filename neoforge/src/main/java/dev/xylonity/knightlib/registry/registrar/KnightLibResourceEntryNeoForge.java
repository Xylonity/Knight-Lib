package dev.xylonity.knightlib.registry.registrar;

import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wraps a {@link DeferredHolder} to implement ResourceEntry exposing both abstracts
 */
public class KnightLibResourceEntryNeoForge<T> implements ResourceEntry<T> {

    private final DeferredHolder<? super T, T> object;

    public KnightLibResourceEntryNeoForge(DeferredHolder<? super T, T> object) {
        this.object = object;
    }

    @Override
    public T get() {
        return object.get();
    }

    @Override
    public ResourceLocation getId() {
        return object.getId();
    }

}