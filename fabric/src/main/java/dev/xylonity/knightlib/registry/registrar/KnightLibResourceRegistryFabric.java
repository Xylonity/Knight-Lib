package dev.xylonity.knightlib.registry.registrar;

import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class KnightLibResourceRegistryFabric<T> implements ResourceRegistry<T> {

    private final String modid;
    private final Registry<T> registry;
    private final List<ResourceEntry<T>> entries = new ArrayList<>();

    public KnightLibResourceRegistryFabric(Registry<T> type, String modid) {
        this.modid = modid;
        this.registry = type;
    }

    @Override
    public <I extends T> ResourceEntry<I> register(String name, Supplier<? extends I> supplier) {
        ResourceLocation id = new ResourceLocation(modid, name);

        I object = supplier.get();
        Registry.register(registry, id, object);
        KnightLibResourceEntryFabric<I> entry = new KnightLibResourceEntryFabric<>(object, id);
        entries.add((ResourceEntry<T>) entry);

        return entry;
    }

    @Override
    public Collection<ResourceEntry<T>> getEntries() {
        return List.copyOf(entries);
    }

    @Override
    public String getNamespace() {
        return this.modid;
    }

    /**
     * Entries are registered through static instancing anyway
     */
    @Override
    public void init() { ;; }

}