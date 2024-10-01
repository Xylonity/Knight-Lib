package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class KnightLibNeoForgePlatform implements KnightLibPlatform {

    @Override
    public <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item) {
        return KnightLib.ITEMS.register(id, item);
    }

}
