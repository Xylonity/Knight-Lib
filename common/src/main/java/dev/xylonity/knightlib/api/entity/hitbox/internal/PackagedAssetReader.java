package dev.xylonity.knightlib.api.entity.hitbox.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xylonity.knightlib.KnightLib;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reads immutable assets from the mod class path on both sides
 */
final class PackagedAssetReader {

    private PackagedAssetReader() {
        ;;
    }

    static JsonObject readJson(ResourceLocation id) {
        final String path = "assets/" + id.getNamespace() + "/" + id.getPath();
        final ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream stream = context == null ? null : context.getResourceAsStream(path);
        if (stream == null) {
            stream = KnightLib.class.getClassLoader().getResourceAsStream(path);
        }
        if (stream == null) {
            throw new IllegalArgumentException("[KnightLib] Packaged asset not found: " + id + " (expected " + path + ")");
        }

        try (final InputStream input = stream; InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("[KnightLib] Could not read packaged asset " + id, exception);
        }

    }

}