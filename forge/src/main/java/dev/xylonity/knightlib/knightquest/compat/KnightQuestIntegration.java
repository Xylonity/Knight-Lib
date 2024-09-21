package dev.xylonity.knightlib.knightquest.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class KnightQuestIntegration {
    private static final String KNIGHTQUEST_MODID = "knightquest";

    // Knight Quest Items
    private static Item knightquestGreatEssence = null;
    private static Item knightquestRadiantEssence = null;
    private static Item knightquestEmptyGoblet = null;
    private static Item knightquestFilledGoblet = null;

    /**
     * Verifies if the `Knight Quest` mod is loaded.
     *
     * @return true if it is loaded, false if not.
     */
    public static boolean isKnightquestLoaded() {
        return ModList.get().isLoaded(KNIGHTQUEST_MODID);
    }

    public static void initialize() {
        if (isKnightquestLoaded()) {
            knightquestGreatEssence = getItem("great_essence");
            knightquestRadiantEssence = getItem("radiant_essence");
            knightquestEmptyGoblet = getItem("empty_goblet");
            knightquestFilledGoblet = getItem("filled_goblet");
        }
    }

    private static Item getItem(String itemName) {
        ResourceLocation itemId = new ResourceLocation(KNIGHTQUEST_MODID, itemName);
        return ForgeRegistries.ITEMS.getValue(itemId);
    }

    public static Item getGreatEssence() {
        return knightquestGreatEssence;
    }

    public static Item getRadiantEssence() {
        return knightquestRadiantEssence;
    }

    public static Item getEmptyGoblet() {
        return knightquestEmptyGoblet;
    }

    public static Item getFilledGoblet() {
        return knightquestFilledGoblet;
    }

}
