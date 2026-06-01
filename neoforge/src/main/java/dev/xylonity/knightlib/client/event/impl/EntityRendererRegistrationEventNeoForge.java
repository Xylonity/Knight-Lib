package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.EntityRendererRegistrationEvent;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityRendererRegistrationEventNeoForge extends EntityRendererRegistrationEvent {

    @Override
    public <T extends Entity> void register(EntityType<T> entityType, EntityRendererProvider<T> rendererProvider) {
        EntityRenderers.register(entityType, rendererProvider);
    }

}