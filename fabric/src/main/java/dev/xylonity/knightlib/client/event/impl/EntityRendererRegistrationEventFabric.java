package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.EntityRendererRegistrationEvent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityRendererRegistrationEventFabric extends EntityRendererRegistrationEvent {

    @Override
    public <T extends Entity> void register(EntityType<T> entityType, EntityRendererProvider<T> rendererProvider) {
        EntityRendererRegistry.register(entityType, rendererProvider);
    }

}