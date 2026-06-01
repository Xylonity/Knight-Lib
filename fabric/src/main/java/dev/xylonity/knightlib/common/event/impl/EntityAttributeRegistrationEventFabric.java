package dev.xylonity.knightlib.common.event.impl;

import dev.xylonity.knightlib.api.event.impl.server.EntityAttributeRegistrationEvent;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.function.Supplier;

public class EntityAttributeRegistrationEventFabric extends EntityAttributeRegistrationEvent {

    @Override
    public <T extends LivingEntity> void register(EntityType<T> entityType, Supplier<AttributeSupplier.Builder> attributes) {
        FabricDefaultAttributeRegistry.register(entityType, attributes.get());
    }

}