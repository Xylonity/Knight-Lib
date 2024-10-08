package dev.xylonity.knightlib.compat.datagen;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.compat.registry.KnightLibItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public class KnightLibLootModifierGenerator extends GlobalLootModifierProvider {

    public KnightLibLootModifierGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String modid) {
        super(output, registries, modid);
    }

    /**
     * Declaration of certain mobs that will drop the small_essence item
     */

    private static final ResourceLocation[] MOB_IDS;

    static {
        String[] vanilla = {
                "creeper", "spider", "skeleton", "zombie", "cave_spider",
                "blaze", "enderman", "ghast", "magma_cube", "phantom",
                "slime", "stray", "vex", "drowned", "witch", "husk",
                "zombie_villager", "wither_skeleton", "pillager",
                "vindicator", "evoker", "hoglin", "piglin"
        };

        String[] knightquest = {
                "gremlin", "eldknight", "samhain", "ratman", "swampman",
                "eldbomb", "lizzy", "bad_patch"
        };

        MOB_IDS = new ResourceLocation[vanilla.length + knightquest.length];

        for (int i = 0; i < vanilla.length; i++) {
            MOB_IDS[i] = ResourceLocation.fromNamespaceAndPath("minecraft", "entities/" + vanilla[i]);
        }

        for (int i = 0; i < knightquest.length; i++) {
            MOB_IDS[vanilla.length + i] = ResourceLocation.fromNamespaceAndPath("knightquest", "entities/" + knightquest[i]);
        }
    }

    @Override
    protected void start() {
        for (ResourceLocation mobId : MOB_IDS) {
            add(mobId.getPath() + "_small_essence", new KnightLibAddItemModifier(new LootItemCondition[]{
                    new LootTableIdCondition.Builder(mobId).build(),
            }, new ItemStack(KnightLibItems.SMALL_ESSENCE.get()).getItem(), 1F));
        }
    }

    @EventBusSubscriber(modid = KnightLibCommon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class KnightLibRecipeGenerator {

        /**
         * Recipe json generator for certain mobs defined on the array MOB_IDS.
         *
         * @see KnightLibLootModifierGenerator
         */

        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
            DataGenerator generator = event.getGenerator();
            PackOutput packOutput = generator.getPackOutput();

            generator.addProvider(event.includeServer(), new KnightLibLootModifierGenerator(packOutput, event.getLookupProvider(), KnightLibCommon.MOD_ID));
        }

    }

}