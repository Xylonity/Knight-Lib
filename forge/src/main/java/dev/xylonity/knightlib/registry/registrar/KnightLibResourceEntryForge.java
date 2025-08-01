package dev.xylonity.knightlib.registry.registrar;

import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;

/**
 * Wraps a RegistryObject to implement ResourceEntry exposing both abstracts
 */
public class KnightLibResourceEntryForge<T> implements ResourceEntry<T> {

    private final RegistryObject<T> object;

    public KnightLibResourceEntryForge(RegistryObject<T> object) {
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