package dev.xylonity.knightlib.registry.registrar;

import dev.xylonity.knightlib.api.registrar.ResourceEntries;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.function.Supplier;

public class KnightLibResourceRegistryNeoForge<T> implements ResourceRegistry<T> {

    private final String modid;
    private final DeferredRegister<T> dr;
    private final ResourceEntries<T> entries = new ResourceEntries<>();

    public KnightLibResourceRegistryNeoForge(DeferredRegister<T> dr, String modid) {
        this.dr = dr;
        this.modid = modid;
    }

    /**
     * Registers with RegistryObject and wraps again into a ResourceEntry
     */
    @Override
    public <I extends T> ResourceEntry<I> register(String name, Supplier<? extends I> supplier) {
        RegistryObject<I> reg = dr.register(name, supplier);
        KnightLibResourceEntryNeoForge<I> entry = new KnightLibResourceEntryNeoForge<>(reg);
        entries.add(entry);
        return entry;
    }

    @Override
    public Collection<ResourceEntry<T>> getEntries() {
        return entries.getAllEntries();
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