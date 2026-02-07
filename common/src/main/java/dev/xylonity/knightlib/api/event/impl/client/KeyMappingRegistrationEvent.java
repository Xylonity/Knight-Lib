package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.KeyMapping;

/**
 * Event to register custom keybindings
 */
public abstract class KeyMappingRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    /**
     * Registers a specific keybind
     * 
     * @param keyMapping the keymapping to register (must be built before registering it)
     */
    public abstract void registerKeyMapping(KeyMapping keyMapping);

    public void registerKeyMappings(KeyMapping... keyMappings) {
        for (KeyMapping keyMapping : keyMappings) {
            registerKeyMapping(keyMapping);
        }

    }

}