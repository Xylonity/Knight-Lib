package dev.xylonity.knightlib.registry;

import com.mojang.serialization.MapCodec;
import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.datagen.KnightLibAddItemModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class KnightLibLootModifier {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KnightLibCommon.MOD_ID);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> ADD_ITEM;

    static {
        ADD_ITEM = LOOT_MODIFIER_SERIALIZERS.register("add_item", KnightLibAddItemModifier.CODEC);
    }

}
