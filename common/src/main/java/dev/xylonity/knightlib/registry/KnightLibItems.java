package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.common.item.*;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class KnightLibItems {

    public static void init() { ;; }

    public static final Supplier<Item> SMALL_ESSENCE = registerSpecificItem("small_essence", new Item.Properties(), ItemType.SMALL_ESSENCE);
    public static final Supplier<Item> GREAT_ESSENCE = registerSpecificItem("great_essence", new Item.Properties(), ItemType.GREAT_ESSENCE);
    public static final Supplier<Item> EMPTY_GRAIL = registerSpecificItem("empty_grail", new Item.Properties(), ItemType.EMPTY_GRAIL);
    public static final Supplier<Item> FILLED_GRAIL = registerSpecificItem("filled_grail", new Item.Properties(), ItemType.FILLED_GRAIL);
    public static final Supplier<Item> HOMUNCULUS = registerItem("homunculus", () -> new HomunculusItem(new Item.Properties()));

    private static <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item) {
        return KnightLibCommon.PLATFORM.registerItem(id, item);
    }

    private static <T extends Item> Supplier<T> registerSpecificItem(String id, Item.Properties properties, ItemType itemType) {
        return KnightLibCommon.PLATFORM.registerSpecificItem(id, properties, itemType);
    }

    public enum ItemType {
        SMALL_ESSENCE,
        GREAT_ESSENCE,
        EMPTY_GRAIL,
        FILLED_GRAIL
    }

}
