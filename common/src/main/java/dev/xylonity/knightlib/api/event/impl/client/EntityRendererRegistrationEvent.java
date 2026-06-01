package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Event to register entity renderers
 */
public abstract class EntityRendererRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    public <T extends Entity> void register(ResourceEntry<EntityType<T>> entityEntry, EntityRendererProvider<T> rendererProvider) {
        register(entityEntry.get(), rendererProvider);
    }

    public abstract <T extends Entity> void register(EntityType<T> entityType, EntityRendererProvider<T> rendererProvider);

}