package dev.xylonity.knightlib.api.automaton.behavior;

import net.minecraft.world.phys.Vec3;

/**
 * Type-safe key generator for data persistence in {@link BehaviorContext} when permuting between automaton states.
 *
 * <p>Keys are identified by their string name and carry a default value
 * that is returned when no value has been stored.</p>
 *
 * @param <T> value type
 */
public record BehaviorData<T>(
        String key,
        T defaultValue
) {

    public static <T> BehaviorData<T> of(String key, T defaultValue) {
        return new BehaviorData<>(key, defaultValue);
    }

    public static BehaviorData<Integer> intKey(String key) {
        return new BehaviorData<>(key, 0);
    }

    public static BehaviorData<Long> longKey(String key) {
        return new BehaviorData<>(key, 0L);
    }

    public static BehaviorData<Float> floatKey(String key) {
        return new BehaviorData<>(key, 0f);
    }

    public static BehaviorData<Double> doubleKey(String key) {
        return new BehaviorData<>(key, 0d);
    }

    public static BehaviorData<Boolean> booleanKey(String key) {
        return new BehaviorData<>(key, false);
    }

    public static BehaviorData<String> stringKey(String key) {
        return new BehaviorData<>(key, "");
    }

    public static BehaviorData<Vec3> vec3Key(String key) {
        return new BehaviorData<>(key, Vec3.ZERO);
    }

    T getDefault() {
        return defaultValue;
    }

}