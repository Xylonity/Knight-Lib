package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.function.Supplier;

/**
 * Event to register entity attributes
 */
public abstract class EntityAttributeRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    public <T extends LivingEntity> void register(ResourceEntry<EntityType<T>> entityEntry, Supplier<AttributeSupplier.Builder> attributes) {
        register(entityEntry.get(), attributes);
    }

    public abstract <T extends LivingEntity> void register(EntityType<T> entityType, Supplier<AttributeSupplier.Builder> attributes);

}