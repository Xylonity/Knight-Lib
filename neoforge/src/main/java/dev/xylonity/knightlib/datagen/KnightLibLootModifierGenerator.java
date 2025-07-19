package dev.xylonity.knightlib.datagen;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class KnightLibLootModifierGenerator extends GlobalLootModifierProvider {

    public KnightLibLootModifierGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registrar) {
        super(output, registrar, KnightLibCommon.MOD_ID);
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