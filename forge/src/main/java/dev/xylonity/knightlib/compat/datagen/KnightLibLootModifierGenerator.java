package dev.xylonity.knightlib.compat.datagen;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.compat.registry.KnightLibItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;

public class KnightLibLootModifierGenerator extends GlobalLootModifierProvider{
    public KnightLibLootModifierGenerator(PackOutput output) {
        super(output, KnightLibCommon.MOD_ID);
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
            MOB_IDS[i] = new ResourceLocation("minecraft", "entities/" + vanilla[i]);
        }

        for (int i = 0; i < knightquest.length; i++) {
            MOB_IDS[vanilla.length + i] = new ResourceLocation("knightquest", "entities/" + knightquest[i]);
        }
    }

    private static final ResourceLocation RATMAN_ID = new ResourceLocation("knightquest", "entities/ratman");
    private static final ResourceLocation LIZZY_ID = new ResourceLocation("knightquest", "entities/lizzy");

    @Override
    protected void start() {

        for (ResourceLocation mobId : MOB_IDS) {
            add(mobId.getPath() + "_small_essence", new KnightLibAddItemModifier(new LootItemCondition[]{
                    new LootTableIdCondition.Builder(mobId).build(),
            }, new ItemStack(KnightLibItems.SMALL_ESSENCE.get()).getItem(), 0.5F));
        }

    }

}