package dev.xylonity.knightlib.api.loot;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Immutable descriptor for an entity loot drop entry
 */
public final class EntityLootEntry {

    private final Supplier<Item> item;
    private final TagKey<EntityType<?>> entityTag;
    private final Supplier<Float> chance;
    private final float minCount;
    private final float maxCount;

    EntityLootEntry(
            Supplier<Item> item,
            TagKey<EntityType<?>> entityTag,
            Supplier<Float> chance,
            float minCount,
            float maxCount
    ) {
        this.item = item;
        this.entityTag = entityTag;
        this.chance = chance;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public Item getItem() {
        return item.get();
    }

    public Supplier<Item> getItemSupplier() {
        return item;
    }

    public TagKey<EntityType<?>> getEntityTag() {
        return entityTag;
    }

    public float getChance() {
        return chance.get();
    }

    public float getMinCount() {
        return minCount;
    }

    public float getMaxCount() {
        return maxCount;
    }

}