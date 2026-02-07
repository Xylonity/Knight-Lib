package dev.xylonity.knightlib.common.event.impl;

import dev.xylonity.knightlib.api.event.impl.server.EntityAttributeRegistrationEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntityAttributeRegistrationEventForge extends EntityAttributeRegistrationEvent {

    private final Map<EntityType<? extends LivingEntity>, AttributeSupplier> attributes = new HashMap<>();

    @Override
    public <T extends LivingEntity> void register(EntityType<T> entityType, Supplier<AttributeSupplier.Builder> attributes) {
        this.attributes.put(entityType, attributes.get().build());
    }

    public void applyToForgeEvent(EntityAttributeCreationEvent forgeEvent) {
        for (Map.Entry<EntityType<? extends LivingEntity>, AttributeSupplier> entry : attributes.entrySet()) {
            forgeEvent.put(entry.getKey(), entry.getValue());
        }

    }

}