package dev.xylonity.knightlib.client.animation;

import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimationMode;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

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

        private static final Map<Map<String, AnimationDefinition>, Map<String, KnightLibAnimation>> CONVERTED = new LinkedHashMap<>(16, 0.75f, true);
        private static final ClassValue<Map<String, KnightLibAnimation>> CONVERTED_CLASSES = new ClassValue<>() {
            @Override
            protected Map<String, KnightLibAnimation> computeValue(Class<?> type) {
                return discover(type);
            }

        };

        private static Map<String, KnightLibAnimation> convert(Map<String, AnimationDefinition> definitions) {
            synchronized (CONVERTED) {
                Map<String, KnightLibAnimation> result = CONVERTED.get(definitions);
                if (result == null) {
                    final Map<String, AnimationDefinition> snapshot = Map.copyOf(definitions);
                    final Map<String, KnightLibAnimation> converted = new LinkedHashMap<>();
                    snapshot.forEach((name, definition) -> converted.put(name, VanillaAnimationAdapter.convert(name, definition)));
                    result = Map.copyOf(converted);
                    CONVERTED.put(snapshot, result);
                    if (CONVERTED.size() > 128) {
                        final var oldest = CONVERTED.entrySet().iterator();
                        oldest.next();
                        oldest.remove();
                    }

                }

                return result;
            }

        }

        private static Map<String, KnightLibAnimation> convert(Class<?> definitions) {
            return CONVERTED_CLASSES.get(definitions);
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