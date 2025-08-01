package dev.xylonity.knightlib.registry.registrar;

import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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

    public KnightLibResourceRegistryFabric(ResourceType type, String modid) {
        this.modid = modid;
        this.registry = switch(type) {
            case BLOCKS -> (Registry<T>) BuiltInRegistries.BLOCK;
            case ITEMS -> (Registry<T>) BuiltInRegistries.ITEM;
            case ENTITIES -> (Registry<T>) BuiltInRegistries.ENTITY_TYPE;
            case BLOCK_ENTITIES -> (Registry<T>) BuiltInRegistries.BLOCK_ENTITY_TYPE;
            case EFFECTS -> (Registry<T>) BuiltInRegistries.MOB_EFFECT;
            case SOUNDS -> (Registry<T>) BuiltInRegistries.SOUND_EVENT;
            case PARTICLES -> (Registry<T>) BuiltInRegistries.PARTICLE_TYPE;
            case CREATIVE_TAB -> (Registry<T>) BuiltInRegistries.CREATIVE_MODE_TAB;
            case MENU -> (Registry<T>) BuiltInRegistries.MENU;
            case STRUCTURE -> (Registry<T>) Registries.STRUCTURE;
            case STRUCTURE_PIECE -> (Registry<T>) BuiltInRegistries.STRUCTURE_PIECE;
            case STRUCTURE_PLACEMENT -> (Registry<T>) BuiltInRegistries.STRUCTURE_PLACEMENT;
            case STRUCTURE_TYPE -> (Registry<T>) BuiltInRegistries.STRUCTURE_TYPE;
            case STRUCTURE_SET -> (Registry<T>) Registries.STRUCTURE_SET;
            case STRUCTURE_POOL_ELEMENT -> (Registry<T>) BuiltInRegistries.STRUCTURE_POOL_ELEMENT;
            case STRUCTURE_PROCESSOR -> (Registry<T>) BuiltInRegistries.STRUCTURE_PROCESSOR;
            case SENSOR_TYPE -> (Registry<T>) BuiltInRegistries.SENSOR_TYPE;
            case DAMAGE_TYPE -> (Registry<T>) Registries.DAMAGE_TYPE;
            case BIOME_SOURCE -> (Registry<T>) BuiltInRegistries.BIOME_SOURCE;
            case BIOME -> (Registry<T>) Registries.BIOME;
            case FLUID -> (Registry<T>) BuiltInRegistries.FLUID;
            case ENCHANTMENTS -> (Registry<T>) BuiltInRegistries.ENCHANTMENT;
            case DIMENSION_TYPE -> (Registry<T>) Registries.DIMENSION_TYPE;
            case DIMENSION -> (Registry<T>) Registries.DIMENSION;
        };

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
     * Entries are registered through static instancing anyways
     */
    @Override
    public void init() { ;; }

}