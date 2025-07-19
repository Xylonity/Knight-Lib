package dev.xylonity.knightlib.datagen;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class KnightLibAddItemModifier extends LootModifier {
    public static final Supplier<MapCodec<KnightLibAddItemModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
                    .and(Registries.ITEM.getCodec()
                            .fieldOf("item")
                            .forGetter(m -> m.item))
                    .and(Codec.FLOAT.fieldOf("chance")
                            .forGetter(m -> m.chance))
                    .apply(inst, KnightLibAddItemModifier::new)));

    private final Item item;
    private final float chance;

    public KnightLibAddItemModifier(LootItemCondition[] conditionsIn, Item item, float chance) {
        super(conditionsIn);
        this.item = item;
        this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(context)) {
                return generatedLoot;
            }
        }

        if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Mob mob && mob.getType().getCategory() == MobCategory.MONSTER
                && context.getRandom().nextFloat() <= KnightLibConfig.SMALL_ESSENCE_DROP_RATE) {
            generatedLoot.add(new ItemStack(KnightLibItems.SMALL_ESSENCE.get()));
        }

        if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Mob mob && mob.getType().getCategory() == MobCategory.MONSTER
                && context.getRandom().nextFloat() <= KnightLibConfig.GREAT_ESSENCE_DROP_RATE) {
            generatedLoot.add(new ItemStack(KnightLibItems.GREAT_ESSENCE.get()));
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}