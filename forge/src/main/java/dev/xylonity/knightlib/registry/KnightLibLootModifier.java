package dev.xylonity.knightlib.registry;

import com.mojang.serialization.Codec;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.datagen.KnightLibAddItemModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class KnightLibLootModifier {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KnightLib.MOD_ID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ADD_ITEM;

    static {
        ADD_ITEM = LOOT_MODIFIER_SERIALIZERS.register("add_item", KnightLibAddItemModifier.CODEC);
    }

}
