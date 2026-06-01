package dev.xylonity.knightlib.api.loot;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Builder for constructing {@link EntityLootEntry} instances.
 *
 * <pre>{@code
 * KnightLibLoot.entityDrop()
 *     .item(() -> CompanionsItems.DEMON_FLESH.get())
 *     .entityTag(CompanionsTags.DEMON_FLESH_DROP)
 *     .chance(() -> (float) CompanionsConfig.DROP_RATE...)
 *     .count(1, 1)
 *     .submit();
 * }</pre>
 */
public final class EntityLootBuilder {

    private Supplier<Item> item;
    private TagKey<EntityType<?>> entityTag;
    private Supplier<Float> chance = () -> 1f;
    private float minCount = 1;
    private float maxCount = 1;

    EntityLootBuilder() {
        ;;
    }

    /**
     * The item to drop
     */
    public EntityLootBuilder item(Supplier<Item> item) {
        this.item = item;
        return this;
    }

    /**
     * The entity tag that determines which entities can drop this item
     */
    public EntityLootBuilder tag(TagKey<EntityType<?>> entityTag) {
        this.entityTag = entityTag;
        return this;
    }

    /**
     * Fixed drop chance (0.0 to 1.0)
     */
    public EntityLootBuilder chance(float chance) {
        this.chance = () -> chance;
        return this;
    }

    public EntityLootBuilder chance(Supplier<Float> chance) {
        this.chance = chance;
        return this;
    }

    /**
     * Drop count range
     */
    public EntityLootBuilder count(float min, float max) {
        this.minCount = min;
        this.maxCount = max;
        return this;
    }

    /**
     * Finalizes and registers this loot entry.
     * Must be called during mod initialization.
     */
    public EntityLootEntry submit() {
        if (item == null) {
            throw new IllegalStateException("[KnightLib] EntityLootBuilder requires an item");
        }
        if (entityTag == null) {
            throw new IllegalStateException("[KnightLib] EntityLootBuilder requires an entityTag");
        }

        final EntityLootEntry entry = new EntityLootEntry(item, entityTag, chance, minCount, maxCount);
        KnightLibLoot.addEntry(entry);

        return entry;
    }

}