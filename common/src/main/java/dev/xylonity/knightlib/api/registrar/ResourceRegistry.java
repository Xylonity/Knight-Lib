package dev.xylonity.knightlib.api.registrar;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Generic registry interface that queues up the registry factories, and then
 * later dispatches them in the loader specific abstractions
 * Example usage:
 * <pre>
 * {@code
 * public static final ResourceRegistry<Block> BLOCKS = ResourceDispatcher.create(ResourceType.BLOCKS, KnightLibCommon.MOD_ID);
 * }
 * </pre>
 */
public interface ResourceRegistry<T> {

    /**
     * Queues a new entry (object, item, etc.) under the given name and instancing
     */
    <I extends T> ResourceEntry<I> register(String name, Supplier<? extends I> object);

    /**
     * Entity registrar helper. Defers the call to the main register abstract
     * @param name name of the entity
     * @param entity entity to register
     * @param category category of the entity
     * @param width width of the entity's bb
     * @param height height of the entity's bb
     * @param properties extra properties of the entity (fire inmmunity, client tracking, etc.)
     * @return the encapsulated instance of the entity
     */
    @SuppressWarnings("unchecked")
    default <X extends Entity> ResourceEntry<EntityType<X>> registerEntity(String name, EntityType.EntityFactory<X> entity, MobCategory category, float width, float height, @Nullable List<Consumer<EntityType.Builder<X>>> properties) {
        return ((ResourceRegistry<EntityType<X>>) this).register(name, () -> {
            EntityType.Builder<X> builder = EntityType.Builder.of(entity, category).sized(width, height);

            if (properties != null) {
                for (Consumer<EntityType.Builder<X>> property : properties) {
                    property.accept(builder);
                }
            }

            return builder.build(new ResourceLocation(getNamespace(), name).toString());
        });
    }

    /**
     * Used internally to dispatch all the entries
     * @return all pending entries in this registry
     */
    Collection<ResourceEntry<T>> getEntries();

    /**
     * Stream of the entry registered once successfully created
     */
    default Stream<T> stream() {
        return getEntries().stream().map(ResourceEntry::get);
    }

    String getNamespace();

    void init();
}