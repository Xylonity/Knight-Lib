package dev.xylonity.knightlib.client.armor;

import dev.xylonity.knightlib.api.client.armor.KnightLibArmorModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class KnightLibForgeArmorModels {

    private static final Map<ResourceLocation, Definition> DEFINITIONS = new HashMap<>();

    private KnightLibForgeArmorModels() {
        ;;
    }

    public static void register(ResourceLocation modelId, ModelLayerLocation layer, Function<ModelPart, KnightLibArmorModel> modelFactory, ResourceLocation texture) {
        DEFINITIONS.put(modelId, new Definition(layer, modelFactory, texture));
    }

    public static Definition get(ResourceLocation modelId) {
        final Definition definition = DEFINITIONS.get(modelId);
        if (definition == null) {
            throw new IllegalStateException("[KnightLib] Armor model is not registered: " + modelId);
        }

        return definition;
    }

    public record Definition(
            ModelLayerLocation layer,
            Function<ModelPart, KnightLibArmorModel> modelFactory,
            ResourceLocation texture
    ) {
        ;;
    }

}
