package dev.xylonity.knightlib.compat.datagen;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xylonity.knightlib.compat.config.values.KnightLibValues;
import dev.xylonity.knightlib.compat.registry.KnightLibItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class KnightLibAddItemModifier extends LootModifier {
    public static final Supplier<MapCodec<KnightLibAddItemModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.mapCodec(inst ->
                    codecStart(inst).and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(KnightLibAddItemModifier::getItem))
                            .and(Codec.FLOAT.fieldOf("chance").forGetter(KnightLibAddItemModifier::getChance))
                            .apply(inst, KnightLibAddItemModifier::new)));

    private final Item item;
    private final float chance;

    public Item getItem() {
        return item;
    }

    public float getChance() {
        return chance;
    }

    public KnightLibAddItemModifier(LootItemCondition[] conditionsIn, Item item, float chance) {
        super(conditionsIn);
        this.item = item;
        this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {

        for(LootItemCondition condition : this.conditions) {
            if(!condition.test(context)) {
                return generatedLoot;
            }
        }

        if (item == KnightLibItems.SMALL_ESSENCE.get() && context.getRandom().nextFloat() <= KnightLibValues.DROP_CHANCE_SMALL_ESSENCE)
            generatedLoot.add(new ItemStack(this.item));

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }

}