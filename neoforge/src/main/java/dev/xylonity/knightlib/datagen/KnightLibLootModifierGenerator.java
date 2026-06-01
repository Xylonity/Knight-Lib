package dev.xylonity.knightlib.datagen;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;

public class KnightLibLootModifierGenerator extends GlobalLootModifierProvider {

    public KnightLibLootModifierGenerator(PackOutput output) {
        super(output, KnightLib.MOD_ID);
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