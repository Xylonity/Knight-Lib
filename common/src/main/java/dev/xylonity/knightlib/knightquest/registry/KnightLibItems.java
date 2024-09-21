package dev.xylonity.knightlib.knightquest.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class KnightLibItems {

    public static void init() { ;; }

    public static final Supplier<Item> SMALL_ESSENCE = registerItem("small_essence", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> GREAT_ESSENCE = registerItem("great_essence", () -> new Item(new Item.Properties()));

    private static <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item) {
        return KnightLibCommon.COMMON_PLATFORM.registerItem(id, item);
    }

}
