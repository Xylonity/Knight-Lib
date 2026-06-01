package dev.xylonity.knightlib.registry;

import com.mojang.serialization.Codec;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.datagen.KnightLibAddItemModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class KnightLibLootModifier {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KnightLib.MOD_ID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ADD_ITEM;

    static {
        ADD_ITEM = LOOT_MODIFIER_SERIALIZERS.register("add_item", KnightLibAddItemModifier.CODEC);
    }

}
