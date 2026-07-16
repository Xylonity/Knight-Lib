package dev.xylonity.knightlib.api.armor;

import dev.xylonity.knightlib.KnightLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Creates armor items whose vanilla model is registered through KnightLib's client event bus
 */
public final class KnightLibArmorItems {

    private static final Map<ResourceLocation, List<ArmorItem>> ITEMS_BY_MODEL = new HashMap<>();

    private KnightLibArmorItems() {
        ;;
    }

    public static ArmorItem create(KnightLibArmorMaterial material, ArmorItem.Type type, Item.Properties properties, ResourceLocation modelId) {
        Objects.requireNonNull(modelId, "modelId");

        final ArmorItem item = KnightLib.PLATFORM.createArmorItem(material, type, properties, modelId);
        ITEMS_BY_MODEL.computeIfAbsent(modelId, ignored -> new ArrayList<>()).add(item);
        return item;
    }

    /**
     * Used by the loader adapters when binding a registered model to its armor items
     */
    public static List<ArmorItem> getRegisteredItems(ResourceLocation modelId) {
        return List.copyOf(ITEMS_BY_MODEL.getOrDefault(modelId, List.of()));
    }

}