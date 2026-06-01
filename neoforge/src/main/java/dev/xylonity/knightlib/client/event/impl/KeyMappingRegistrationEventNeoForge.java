package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.KeyMappingRegistrationEvent;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

import java.util.ArrayList;
import java.util.List;

public class KeyMappingRegistrationEventNeoForge extends KeyMappingRegistrationEvent {

    private final List<KeyMapping> keyMappings = new ArrayList<>();

    @Override
    public void registerKeyMapping(KeyMapping keyMapping) {
        keyMappings.add(keyMapping);
    }

    public void applyToForgeEvent(RegisterKeyMappingsEvent forgeEvent) {
        for (KeyMapping keyMapping : keyMappings) {
            forgeEvent.register(keyMapping);
        }

    }

}