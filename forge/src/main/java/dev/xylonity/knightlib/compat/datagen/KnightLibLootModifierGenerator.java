package dev.xylonity.knightlib.compat.datagen;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.compat.registry.KnightLibItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class KnightLibLootModifierGenerator extends GlobalLootModifierProvider {

    public KnightLibLootModifierGenerator(DataGenerator gen, String modid) {
        super(gen, modid);
    }

    /**
     * Declaration of certain mobs that will drop the small_essence item
     */

    private static final ResourceLocation[] MOB_IDS = {
            new ResourceLocation("minecraft", "entities/creeper"),
            new ResourceLocation("minecraft", "entities/spider"),
            new ResourceLocation("minecraft", "entities/skeleton"),
            new ResourceLocation("minecraft", "entities/zombie"),
            new ResourceLocation("minecraft", "entities/cave_spider"),
            new ResourceLocation("minecraft", "entities/blaze"),
            new ResourceLocation("minecraft", "entities/enderman"),
            new ResourceLocation("minecraft", "entities/ghast"),
            new ResourceLocation("minecraft", "entities/magma_cube"),
            new ResourceLocation("minecraft", "entities/phantom"),
            new ResourceLocation("minecraft", "entities/slime"),
            new ResourceLocation("minecraft", "entities/stray"),
            new ResourceLocation("minecraft", "entities/vex"),
            new ResourceLocation("minecraft", "entities/drowned"),
            new ResourceLocation("knightquest", "entities/gremlin"),
            new ResourceLocation("knightquest", "entities/eldknight"),
            new ResourceLocation("knightquest", "entities/samhain"),
            new ResourceLocation("knightquest", "entities/ratman"),
            new ResourceLocation("knightquest", "entities/swampman"),
            new ResourceLocation("knightquest", "entities/eldbomb"),
            new ResourceLocation("knightquest", "entities/lizzy"),
            new ResourceLocation("knightquest", "entities/bad_patch")
    };

    @Override
    protected void start() {

        for (ResourceLocation mobId : MOB_IDS) {
            add(mobId.getPath() + "_small_essence", new KnightLibAddItemModifier(new LootItemCondition[]{
                    new LootTableIdCondition.Builder(mobId).build(),
            }, new ItemStack(KnightLibItems.SMALL_ESSENCE.get()).getItem(), 0.5F));
        }

    }

    @Mod.EventBusSubscriber(modid = KnightLibCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public class KnightLibEventRegisters {

        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
            DataGenerator generator = event.getGenerator();

            generator.addProvider(event.includeServer(), new KnightLibLootModifierGenerator(generator, KnightLibCommon.MOD_ID));
        }

    }

}