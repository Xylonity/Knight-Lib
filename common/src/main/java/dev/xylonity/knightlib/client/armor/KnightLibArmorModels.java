package dev.xylonity.knightlib.client.armor;

import dev.xylonity.knightlib.api.client.armor.KnightLibArmorModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Loader agnostic registry for vanilla armor model layers. Fabric and Forge fill this table through their own registration hooks.
 */
public final class KnightLibArmorModels {

    private static final Map<ResourceLocation, Definition> DEFINITIONS = new HashMap<>();

    public static void register(ResourceLocation modelId, ModelLayerLocation layer, Function<ModelPart, KnightLibArmorModel> modelFactory, ResourceLocation texture) {
        DEFINITIONS.put(
                Objects.requireNonNull(modelId, "modelId"),
                new Definition(
                        Objects.requireNonNull(layer, "layer"),
                        Objects.requireNonNull(modelFactory, "modelFactory"),
                        Objects.requireNonNull(texture, "texture")
                )

        );

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
            ResourceLocation texture) {
        ;;
    }

}
