package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.resources.ResourceLocation;

/**
 * Event to register additional models that must be loaded. Useful for items that use custom models or items that do not
 * have an associated block.
 */
public abstract class AdditionalModelsRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    public void registerItemModel(String modid, String path) {
        register(new ResourceLocation(modid, "item/" + path));
    }

    public void registerBlockModel(String modid, String path) {
        register(new ResourceLocation(modid, "block/" + path));
    }

    /**
     * Registers and loads an additional model.
     *
     * @param modelLocation the path of the model (ResourceLocation(mod_id, "item/custom_model"))
     */
    public abstract void register(ResourceLocation modelLocation);

}