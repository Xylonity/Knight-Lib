package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.AdditionalModelsRegistrationEvent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.ArrayList;
import java.util.List;

public class AdditionalModelsRegistrationEventNeoForge extends AdditionalModelsRegistrationEvent {

    private final List<ResourceLocation> models = new ArrayList<>();

    @Override
    public void register(ResourceLocation modelLocation) {
        models.add(modelLocation);
    }

    public void applyToForgeEvent(ModelEvent.RegisterAdditional forgeEvent) {
        for (ResourceLocation model : models) {
            forgeEvent.register(new ModelResourceLocation(model, "standalone"));
        }

    }

}