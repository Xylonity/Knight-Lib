package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.client.armor.KnightLibArmorModel;
import dev.xylonity.knightlib.api.event.impl.client.ArmorModelRegistrationEvent;
import dev.xylonity.knightlib.client.armor.KnightLibArmorModels;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ArmorModelRegistrationEventFabric extends ArmorModelRegistrationEvent {

    @Override
    public void register(ResourceLocation modelId, ModelLayerLocation layer, Supplier<LayerDefinition> layerDefinition, Function<ModelPart, KnightLibArmorModel> modelFactory, ResourceLocation texture) {
        KnightLibArmorModels.register(modelId, layer, modelFactory, texture);
        EntityModelLayerRegistry.registerModelLayer(layer, layerDefinition::get);
    }

}
