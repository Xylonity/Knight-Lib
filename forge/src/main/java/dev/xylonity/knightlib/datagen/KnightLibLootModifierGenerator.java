package dev.xylonity.knightlib.datagen;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;

public class KnightLibLootModifierGenerator extends GlobalLootModifierProvider {

    public KnightLibLootModifierGenerator(DataGenerator output) {
        super(output, KnightLibCommon.MOD_ID);
    }

    @Override
    protected void start() {
        add("essence_all_hostiles", new KnightLibAddItemModifier(
                new LootItemCondition[]{},
                KnightLibItems.SMALL_ESSENCE.get(),
                (float) KnightLibConfig.SMALL_ESSENCE_DROP_RATE
        ));
    }

}