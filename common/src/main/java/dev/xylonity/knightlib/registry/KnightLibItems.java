package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.common.item.EmptyGrailItem;
import dev.xylonity.knightlib.common.item.GreatEssenceItem;
import dev.xylonity.knightlib.common.item.KnightLibItem;
import dev.xylonity.knightlib.common.item.SmallEssenceItem;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class KnightLibItems {

    public static void init() { ;; }

    public static final Supplier<Item> SMALL_ESSENCE = registerItem("small_essence", () -> new SmallEssenceItem(new Item.Properties()));
    public static final Supplier<Item> GREAT_ESSENCE = registerItem("great_essence", () -> new GreatEssenceItem(new Item.Properties()));
    public static final Supplier<Item> EMPTY_GRAIL = registerItem("empty_grail", () -> new EmptyGrailItem(new Item.Properties()));
    public static final Supplier<Item> FILLED_GRAIL = registerItem("filled_grail", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HOMUNCULUS = registerItem("homunculus", () -> new KnightLibItem(new Item.Properties(), "homunculus"));

    private static <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item) {
        return KnightLibCommon.PLATFORM.registerItem(id, item);
    }

}
