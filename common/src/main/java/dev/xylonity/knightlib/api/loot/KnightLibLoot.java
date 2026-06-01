package dev.xylonity.knightlib.api.loot;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fast entity loot drop registration API.
 *
 * For more complex loot table modifications (chest loot, custom conditions, etc.), use
 * {@link dev.xylonity.knightlib.api.event.impl.server.LootTableModifyEvent} event.</p>
 */
public final class KnightLibLoot {

    private static final List<EntityLootEntry> ENTITY_ENTRIES = new ArrayList<>();

    private KnightLibLoot() {
        ;;
    }

    /**
     * Creates a builder for a new entity loot drop entry
     */
    public static EntityLootBuilder builder() {
        return new EntityLootBuilder();
    }

    /**
     * Adds a finalized entry to the registry. Internal
     */
    static void addEntry(EntityLootEntry entry) {
        ENTITY_ENTRIES.add(entry);
    }

    /**
     * Returns all registered entity loot entries. Internal
     */
    public static List<EntityLootEntry> getEntityEntries() {
        return Collections.unmodifiableList(ENTITY_ENTRIES);
    }

    /**
     * Builds a {@link LootPool.Builder} for a given entity loot entry.
     * The pool includes an entity-tag condition and a random-chance condition,
     * and is safe to inject into any {@code entities/*} loot table.
     */
    public static LootPool.Builder buildPool(EntityLootEntry entry) {

        float min = entry.getMinCount();
        float max = entry.getMaxCount();

        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(entry.getItem())
                        .apply(SetItemCountFunction.setCount(
                                min == max ? ConstantValue.exactly(min) : UniformGenerator.between(min, max)
                        )))
                .when(LootItemRandomChanceCondition.randomChance(entry.getChance()))
                .when(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity().of(entry.getEntityTag()).build()
                ));
    }

}