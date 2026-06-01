package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.AdditionalModelsRegistrationEvent;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class AdditionalModelsRegistrationEventFabric extends AdditionalModelsRegistrationEvent {

    private final List<ResourceLocation> models = new ArrayList<>();

    @Override
    public void register(ResourceLocation modelLocation) {
        models.add(modelLocation);
    }

    public void applyToFabric() {
        ModelLoadingPlugin.register(pluginContext -> {
            for (ResourceLocation model : models) {
                pluginContext.addModels(model);
            }

        });

    }

}