package dev.xylonity.knightlib.datagen;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

import java.util.concurrent.CompletableFuture;

public class KnightLibLootModifierGenerator extends GlobalLootModifierProvider {

    public KnightLibLootModifierGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, KnightLib.MOD_ID);
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
