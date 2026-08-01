package dev.xylonity.knightlib.client.animation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.client.animation.geo.GeoModelDefinition;
import dev.xylonity.knightlib.client.animation.geo.GeoParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Client-side cache of parsed geo assets
 */
public final class KnightLibAnimationAssets {

    private static final Object LOCK = new Object();

    private static final Map<ResourceLocation, GeoModelDefinition> MODELS = new HashMap<>();
    private static final Map<ResourceLocation, Map<String, KnightLibAnimation>> ANIMATIONS = new HashMap<>();

    private static volatile ResourceManager resourceManager;
    private static volatile int generation;

    private KnightLibAnimationAssets() {
        ;;
    }

    public static void reload(ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        synchronized (LOCK) {
            KnightLibAnimationAssets.resourceManager = resourceManager;
            MODELS.clear();
            ANIMATIONS.clear();
            KnightLibAnimator.clearWarnings();
            generation++;
        }

    }

    /**
     * Bumped on every asset reload
     */
    public static int generation() {
        return generation;
    }

    public static GeoModelDefinition getModel(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        synchronized (LOCK) {
            GeoModelDefinition definition = MODELS.get(id);
            if (definition == null) {
                definition = loadModel(id);
                MODELS.put(id, definition);
            }

            return definition;
        }

    }

    /**
     * Resolves an animation by exact key or by its last dot separated segment
     */
    public static KnightLibAnimation getAnimation(ResourceLocation file, String name) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(name, "name");

        Map<String, KnightLibAnimation> animations;
        synchronized (LOCK) {
            animations = ANIMATIONS.get(file);
            if (animations == null) {
                animations = loadAnimations(file);
                ANIMATIONS.put(file, animations);
            }

        }

        final KnightLibAnimation exact = animations.get(name);
        if (exact != null) {
            return exact;
        }

        KnightLibAnimation match = null;
        for (final Map.Entry<String, KnightLibAnimation> entry : animations.entrySet()) {
            final String key = entry.getKey();
            final int lastDot = key.lastIndexOf('.');
            if (lastDot >= 0 && key.substring(lastDot + 1).equals(name)) {
                if (match != null) {
                    // Ambiguous
                    return null;
                }

                match = entry.getValue();
            }

        }

        return match;
    }

    private static GeoModelDefinition loadModel(ResourceLocation id) {
        final Resource resource = findResource(id, "Geo model");
        try (BufferedReader reader = resource.openAsReader()) {
            final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return GeoParser.parseModel(json);
        }
        catch (Exception exception) {
            throw new IllegalStateException("[KnightLib] Failed to parse requested geo model " + id + " at " + assetPath(id), exception);
        }

    }

    private static Map<String, KnightLibAnimation> loadAnimations(ResourceLocation file) {
        final Resource resource;
        try {
            resource = findResource(file, "Animation file");
        }
        catch (IllegalStateException exception) {
            KnightLib.LOGGER.error(exception.getMessage());
            return Map.of();
        }

        try (final BufferedReader reader = resource.openAsReader()) {
            final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return Map.copyOf(GeoParser.parseAnimations(json));
        }
        catch (Exception exception) {
            KnightLib.LOGGER.error("[KnightLib] Failed to parse requested animation file {} at {}", file, assetPath(file), exception);
            return Map.of();
        }

    }

    private static Resource findResource(ResourceLocation id, String kind) {
        final ResourceManager manager = resourceManager;
        if (manager == null) {
            throw new IllegalStateException("[KnightLib] " + kind + " requested before the client resource manager was ready: " + id);
        }

        return manager.getResource(id).orElseThrow(() -> new IllegalStateException("[KnightLib] " + kind + " not found: " + id + " (expected at " + assetPath(id) + ")"));
    }

    private static String assetPath(ResourceLocation id) {
        return "assets/" + id.getNamespace() + "/" + id.getPath();
    }

}