package dev.xylonity.knightlib.api.automaton;

import dev.xylonity.knightlib.api.automaton.behavior.BehaviorContext;

import javax.annotation.Nullable;

/**
 * A global transition rule evaluated every tick before the active behavior.
 *
 * <p>Use this for conditions that should apply regardless of the current state.
 * Writing the check once here replaces duplicating it in every behavior.</p>
 *
 * <p>Another solution that occurred to me was to create a lightweight global behavior
 * that would execute global logic, but I think that would be bloated for the purpose it serves.</p>
 *
 * <p>Global rules are evaluated in registration order. The first one that returns
 * a non-null state wins and triggers an interrupted transition.</p>
 *
 * @param <E> the entity in this context
 * @param <S> state enum type
 */
@FunctionalInterface
public interface GlobalRule<E, S extends Enum<S>> {

    /**
     * Evaluated every tick before the active behavior runs
     *
     * @param entity the entity
     * @param context current behavior context
     * @param currentState the active state
     * @return target state to interrupt into, or {@code null} to do nothing
     */
    @Nullable
    S evaluate(E entity, BehaviorContext<S> context, S currentState);

}