package dev.xylonity.knightlib.api.util;

import net.minecraft.resources.ResourceLocation;

/**
 * API for creating ResourceLocations across different versions
 */
@SuppressWarnings("removal")
public final class ResourceLocations {

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation minecraft(String path) {
        return of(ResourceLocation.DEFAULT_NAMESPACE, path);
    }

    public static ResourceLocation parse(String location) {
        return new ResourceLocation(location);
    }

    public static ResourceLocation tryBuild(String namespace, String path) {
        try {
            return of(namespace, path);
        }
        catch (RuntimeException exception) {
            return null;
        }

    }

    public static ResourceLocation tryParse(String location) {
        try {
            return parse(location);
        }
        catch (RuntimeException exception) {
            return null;
        }

    }

}
