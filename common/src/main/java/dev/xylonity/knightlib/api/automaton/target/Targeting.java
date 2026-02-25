package dev.xylonity.knightlib.api.automaton.target;

import net.minecraft.world.damagesource.DamageSource;

import javax.annotation.Nullable;

/**
 * Pluggable targeting system for the Automaton.
 *
 * <p>Allows custom target acquisition strategies (proximity, line-of-sight, hurt by, etc.)
 * to be composed without relying on vanilla target goals.</p>
 *
 * @param <E> entity in this context
 */
@FunctionalInterface
public interface Targeting<E> {

    default void onStart(E entity) {
        ;;
    }

    void tick(E entity);

    default void onStop(E entity) {
        ;;
    }

    default void onDamaged(E entity, @Nullable DamageSource source, float amount) {
        ;;
    }

}