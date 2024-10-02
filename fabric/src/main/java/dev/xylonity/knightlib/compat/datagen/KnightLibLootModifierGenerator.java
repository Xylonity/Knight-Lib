package dev.xylonity.knightlib.compat.datagen;

import dev.xylonity.knightlib.compat.config.values.KnightLibValues;
import dev.xylonity.knightlib.compat.registry.KnightLibItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class KnightLibLootModifierGenerator {

    /**
     * Declaration of certain mobs that will drop the small_essence item
     */

    private static final ResourceLocation[] MOB_IDS;

    static {
        String[] vanilla = {
                "creeper", "spider", "skeleton", "zombie", "cave_spider",
                "blaze", "enderman", "ghast", "magma_cube", "phantom",
                "slime", "stray", "vex", "drowned", "witch", "husk",
                "zombie_villager", "wither_skeleton", "pillager",
                "vindicator", "evoker", "hoglin", "piglin"
        };

        String[] knightquest = {
                "gremlin", "eldknight", "samhain", "ratman", "swampman",
                "eldbomb", "lizzy", "bad_patch"
        };

        MOB_IDS = new ResourceLocation[vanilla.length + knightquest.length];

        for (int i = 0; i < vanilla.length; i++) {
            MOB_IDS[i] = ResourceLocation.fromNamespaceAndPath("minecraft", "entities/" + vanilla[i]);
        }

        for (int i = 0; i < knightquest.length; i++) {
            MOB_IDS[vanilla.length + i] = ResourceLocation.fromNamespaceAndPath("knightquest", "entities/" + knightquest[i]);
        }
    }

    public static void modifyLootTables() {

        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder) -> {
            for (ResourceLocation mobId : MOB_IDS) {
                if (mobId.equals(resourceManager.location())) {
                    LootPool.Builder poolBuilder = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(KnightLibItems.SMALL_ESSENCE.get())
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 1.0f)))
                            )
                            .when(LootItemRandomChanceCondition.randomChance(KnightLibValues.DROP_CHANCE_SMALL_ESSENCE));
                    lootManager.pool(poolBuilder.build());
                    break;
                }
            }

        });

    }

}