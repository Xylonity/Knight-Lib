package dev.xylonity.knightlib.registry.registrar;

import java.util.Collection;
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

    void init();
}