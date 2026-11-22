package dev.xylonity.knightlib.datagen;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.util.ResourceLocations;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class KnightLibLootModifierGenerator {

    public static void init() {
        KnightLibEssenceDropCondition.register();

        LootTableEvents.MODIFY.register((id, tableBuilder, source) -> {
            if (!id.location().getPath().startsWith("entities/")) {
                return;
            }

            ResourceLocation resourceLocation = ResourceLocations.of(id.location().getNamespace(), id.location().getPath().substring("entities/".length()));

            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(resourceLocation)) {
                return;
            }

            if (!KnightLib.isEnabled(KnightLib.Usage.GREEN_ESSENCES)) {
                return;
            }

            LootPool.Builder smallEssencePool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(KnightLibItems.SMALL_ESSENCE.get())
                            .apply(SetItemCountFunction.setCount(
                                    UniformGenerator.between(1.0F, 1.0F)
                            ))
                    )
                    .when(() -> new KnightLibEssenceDropCondition(false));

            LootPool.Builder greatEssencePool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(KnightLibItems.GREAT_ESSENCE.get())
                            .apply(SetItemCountFunction.setCount(
                                    UniformGenerator.between(1.0F, 1.0F)
                            ))
                    )
                    .when(() -> new KnightLibEssenceDropCondition(true));

            tableBuilder.withPool(smallEssencePool).withPool(greatEssencePool);
        });

    }

}
