package dev.xylonity.knightlib.api.registrar;

import dev.xylonity.knightlib.KnightLib;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
     * Entity registrar helper.
     *
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

    default <X extends Entity> ResourceEntry<EntityType<X>> registerEntity(String name, EntityType.EntityFactory<X> entity, MobCategory category, float width, float height) {
        return this.registerEntity(name, entity, category, width, height, null);
    }

    /**
     * Menu registrar helper.
     *
     * @param name registry name
     * @param factory menu constructor (syncId, playerInv, buf)
     * @return the encapsulated MenuType entry
     */
    @SuppressWarnings("unchecked")
    default <M extends AbstractContainerMenu> ResourceEntry<MenuType<M>> registerMenu(String name, MenuFactory<M> factory) {
        return ((ResourceRegistry<MenuType<M>>) this).register(name, () ->
                KnightLib.PLATFORM.createMenuFactory(factory)
        );

    }

    /**
     * BlockEntity registrar helper.
     *
     * @param name registry name
     * @param factory factory of the block entity to register
     * @param block block linked to that block entity
     * @return the encapsulated BlockEntityType
     */
    @SuppressWarnings("unchecked")
    default <M extends BlockEntity> ResourceEntry<BlockEntityType<M>> registerBlockEntity(String name, ResourceRegistry.BlockEntityFactory<M> factory, Supplier<Block> block) {
        return ((ResourceRegistry<BlockEntityType<M>>) this).register(name, () ->
                KnightLib.PLATFORM.createBlockEntityType(factory, block)
        );

    }

    /**
     * BlockEntity registrar helper.
     *
     * @param name registry name
     * @param factory factory of the block entity to register
     * @param blocks blocks linked to that block entity
     * @return the encapsulated BlockEntityType
     */
    @SuppressWarnings("unchecked")
    default <M extends BlockEntity> ResourceEntry<BlockEntityType<M>> registerBlockEntity(String name, ResourceRegistry.BlockEntityFactory<M> factory, Supplier<Block>... blocks) {
        return ((ResourceRegistry<BlockEntityType<M>>) this).register(name, () ->
                KnightLib.PLATFORM.createBlockEntityType(factory, blocks)
        );

    }

    @FunctionalInterface
    interface MenuFactory<M extends AbstractContainerMenu> {
        M create(int syncId, Inventory playerInv, FriendlyByteBuf buf);
    }

    @FunctionalInterface
    interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
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