package dev.xylonity.knightlib.client.animation;

import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimationMode;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tells a renderer where named animations come from.
 */
@FunctionalInterface
public interface KnightLibAnimationSource {

    KnightLibAnimation get(String name);

    /**
     * A geo (bedrock format) animation file
     */
    static KnightLibAnimationSource geo(ResourceLocation file) {
        return name -> KnightLibAnimationAssets.getAnimation(file, name);
    }

    /**
     * A coded vanilla animation descriptor
     */
    static KnightLibAnimationSource vanilla(Map<String, AnimationDefinition> definitions) {
        Map<String, KnightLibAnimation> converted = Cache.convert(definitions);
        return converted::get;
    }

    static KnightLibAnimationSource vanilla(Class<?> definitions) {
        Map<String, KnightLibAnimation> converted = Cache.convert(definitions);
        return converted::get;
    }

    static KnightLibAnimationSource none() {
        return name -> null;
    }

    final class Cache {

        private static final Map<Map<String, AnimationDefinition>, Map<String, KnightLibAnimation>> CONVERTED = new IdentityHashMap<>();
        private static final Map<Class<?>, Map<String, KnightLibAnimation>> CONVERTED_CLASSES = new IdentityHashMap<>();

        private static Map<String, KnightLibAnimation> convert(Map<String, AnimationDefinition> definitions) {
            synchronized (CONVERTED) {
                return CONVERTED.computeIfAbsent(definitions, source -> {
                    final Map<String, KnightLibAnimation> converted = new LinkedHashMap<>();
                    for (final Map.Entry<String, AnimationDefinition> entry : source.entrySet()) {
                        converted.put(entry.getKey(), VanillaAnimationAdapter.convert(entry.getKey(), entry.getValue()));
                    }

                    return Map.copyOf(converted);
                });

            }

        }

        private static Map<String, KnightLibAnimation> convert(Class<?> definitions) {
            synchronized (CONVERTED_CLASSES) {
                return CONVERTED_CLASSES.computeIfAbsent(definitions, Cache::discover);
            }

        }

        private static Map<String, KnightLibAnimation> discover(Class<?> definitions) {
            final Field[] fields = definitions.getDeclaredFields();

            Arrays.sort(fields, Comparator.comparing(Field::getName));

            final Map<String, KnightLibAnimation> converted = new LinkedHashMap<>();
            for (final Field field : fields) {
                final int modifiers = field.getModifiers();
                if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || field.getType() != AnimationDefinition.class) {
                    continue;
                }

                final String name = field.getName().toLowerCase(Locale.ROOT);
                try {
                    if (!field.canAccess(null) && !field.trySetAccessible()) {
                        throw new IllegalAccessException("[KnightLib] Field is not accessible");
                    }

                    final AnimationDefinition definition = (AnimationDefinition) field.get(null);
                    final KnightLibAnimationMode mode = field.getAnnotation(KnightLibAnimationMode.class);
                    final KnightLibAnimation animation = VanillaAnimationAdapter.convert(name, definition, mode == null ? null : mode.value());
                    if (converted.put(name, animation) != null) {
                        throw new IllegalArgumentException("Duplicate Java animation name: " + name);
                    }

                }
                catch (Exception exception) {
                    throw new IllegalArgumentException("[KnightLib] Cannot read animation field " + definitions.getName() + "." + field.getName(), exception);
                }

            }

            if (converted.isEmpty()) {
                throw new IllegalArgumentException("[KnightLib] " + definitions.getName() + " declares no public static final AnimationDefinition fields");
            }

            return Map.copyOf(converted);
        }

    }

}