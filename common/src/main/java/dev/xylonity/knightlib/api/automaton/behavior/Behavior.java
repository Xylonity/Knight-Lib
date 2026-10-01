package dev.xylonity.knightlib.api.automaton.behavior;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Pluggable state-driven AI behavior for the Automaton system.
 *
 * <p>A {@code Behavior} drives an entity while its associated state is active.
 * It may request transitions to other states by returning a target state from {@link #tick}.</p>
 *
 * <h4>Lifecycle</h4>
 * <ol>
 *   <li>{@link #canStart} is checked before transitioning into this state</li>
 *   <li>{@link #onEnter} is called once when the state becomes active</li>
 *   <li>{@link #onFirstTick} is called on the first tick after entering</li>
 *   <li>{@link #tick} is called every tick</li>
 *   <li>{@link #onTickEnd} is called after tick processing</li>
 *   <li>{@link #onExit} is called when leaving this state</li>
 * </ol>
 *
 * @param <E> entity in this context
 * @param <S> states of the entity
 *
 * @author Xylonity
 */
@FunctionalInterface
public interface Behavior<E, S extends Enum<S>> {

    @Nullable
    S tick(E entity, BehaviorContext<S> context);

    default boolean canStart(E entity, BehaviorContext<S> context) {
        return true;
    }

    default boolean canBeInterrupted(E entity, BehaviorContext<S> context, S interruptingState) {
        return true;
    }

    @Nullable
    default S shouldForceExit(E entity, BehaviorContext<S> context) {
        return null;
    }

    default void onEnter(E entity, BehaviorContext<S> context) {
        ;;
    }

    default void onFirstTick(E entity, BehaviorContext<S> context) {
        ;;
    }

    default void onTickEnd(E entity, BehaviorContext<S> context) {
        ;;
    }

    default void onExit(E entity, BehaviorContext<S> context, boolean interrupted) {
        ;;
    }

    default void onDeath(E entity, BehaviorContext<S> context) {
        ;;
    }

    default void onEffectAdded(E entity, BehaviorContext<S> context, @Nullable MobEffectInstance effectInstance) {
        ;;
    }

    default void onTargetChanged(E entity, BehaviorContext<S> context, @Nullable LivingEntity previous, @Nullable LivingEntity current) {
        ;;
    }

    @Nullable
    default S onDamaged(E entity, BehaviorContext<S> context, float amount) {
        return null;
    }

}