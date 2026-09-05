package dev.xylonity.knightlib.api.animation.internal;

import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Precomputes unambiguous short names, while an exact name always takes precedence
 */
public final class AnimationLookup {

    public static Map<String, KnightLibAnimation> withAliases(Map<String, KnightLibAnimation> animations) {
        final Map<String, KnightLibAnimation> indexed = new HashMap<>(animations);
        final Set<String> extrange = new HashSet<>();
        for (final Map.Entry<String, KnightLibAnimation> entry : animations.entrySet()) {
            final int dot = entry.getKey().lastIndexOf('.');
            if (dot < 0) {
                continue;
            }

            final String name = entry.getKey().substring(dot + 1);
            if (!animations.containsKey(name) && indexed.putIfAbsent(name, entry.getValue()) != null) {
                extrange.add(name);
            }

        }

        extrange.forEach(indexed::remove);

        return Map.copyOf(indexed);
    }

}