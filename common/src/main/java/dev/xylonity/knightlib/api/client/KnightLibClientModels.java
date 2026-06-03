package dev.xylonity.knightlib.api.client;

import dev.xylonity.knightlib.api.event.impl.client.AdditionalModelsRegistrationEvent;
import dev.xylonity.knightlib.client.platform.KnightLibClientPlatform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.ServiceLoader;

/**
 * Client side helper for retrieving models registered through {@link AdditionalModelsRegistrationEvent}
 * <p>
 * Additional models are stored under a different model variant depending on the loader (thanks fabric), so using
 * the {@link #getAdditionalModel(ResourceLocation)} method is mandatory.
 */
public final class KnightLibClientModels {

    private static final KnightLibClientPlatform PLATFORM = ServiceLoader.load(KnightLibClientPlatform.class).findFirst().orElseThrow();

    private KnightLibClientModels() {
        ;;
    }

    /**
     * Resolves the baked model previously registered as an additional model.
     *
     * @param modelLocation the model location {@code clockwork:item/yeah}
     * @return the baked model, or the missing model if it has not been loaded
     */
    public static BakedModel getAdditionalModel(ResourceLocation modelLocation) {
        return PLATFORM.getAdditionalModel(modelLocation);
    }

}
