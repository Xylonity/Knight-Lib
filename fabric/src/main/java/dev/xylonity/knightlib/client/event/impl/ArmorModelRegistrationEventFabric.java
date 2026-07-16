package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.armor.KnightLibArmorItems;
import dev.xylonity.knightlib.api.client.armor.KnightLibArmorModel;
import dev.xylonity.knightlib.api.event.impl.client.ArmorModelRegistrationEvent;
import dev.xylonity.knightlib.client.armor.KnightLibFabricArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ArmorModelRegistrationEventFabric extends ArmorModelRegistrationEvent {

    @Override
    public void register(ResourceLocation modelId, ModelLayerLocation layer, Supplier<LayerDefinition> layerDefinition, Function<ModelPart, KnightLibArmorModel> modelFactory, ResourceLocation texture) {
        final List<ArmorItem> items = KnightLibArmorItems.getRegisteredItems(modelId);
        if (items.isEmpty()) {
            throw new IllegalStateException("[KnightLib] No armor items use model " + modelId);
        }

        EntityModelLayerRegistry.registerModelLayer(layer, layerDefinition::get);
        ArmorRenderer.register(
                new KnightLibFabricArmorRenderer(layer, modelFactory, texture),
                items.toArray(Item[]::new)
        );

    }

}
