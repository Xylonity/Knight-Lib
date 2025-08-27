package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.item.*;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceType;
import net.minecraft.world.item.Item;

public class KnightLibItems {

    public static final ResourceRegistry<Item> ITEMS = ResourceDispatcher.create(ResourceType.ITEMS, KnightLib.MOD_ID);

    public static final ResourceEntry<Item> SMALL_ESSENCE = ITEMS.register("small_essence", () -> new SmallEssenceItem(new Item.Properties()));
    public static final ResourceEntry<Item> GREAT_ESSENCE = ITEMS.register("great_essence", () -> new GreatEssenceItem(new Item.Properties()));
    public static final ResourceEntry<Item> EMPTY_GRAIL = ITEMS.register("empty_grail", () -> new EmptyGrailItem(new Item.Properties()));
    public static final ResourceEntry<Item> FILLED_GRAIL = ITEMS.register("filled_grail", () -> new FilledGrailItem(new Item.Properties()));
    public static final ResourceEntry<Item> HOMUNCULUS = ITEMS.register("homunculus", () -> new HomunculusItem(new Item.Properties()));

    public static final ResourceEntry<Item> GREAT_CHALICE = ITEMS.register("great_chalice", KnightLib.PLATFORM.createItem("great_chalice", new Item.Properties(), ItemType.GREAT_CHALICE));

    public enum ItemType {
        GREAT_CHALICE
    }

}
