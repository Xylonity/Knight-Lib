package dev.xylonity.knightlib.platform;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public interface KnightLibPlatform {

    <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item);

}
