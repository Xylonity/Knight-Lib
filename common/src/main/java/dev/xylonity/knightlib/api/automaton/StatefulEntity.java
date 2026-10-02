package dev.xylonity.knightlib.api.automaton;

import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

/**
 * Common entry point for a {@link Mob} driven by an {@link Automaton}.
 *
 * The implementor holds a single {@link AutomatonHandler}, calls its {@code tick} every tick, forwards the damage, death,
 * target and effect hooks to it and registers a {@link dev.xylonity.knightlib.api.automaton.goal.StateMachineGoal} resolving
 * {@link #getAutomaton()}. {@link dev.xylonity.knightlib.common.entity.StatefulHostileEntity} is a complete implementation.
 *
 * @param <E> entity type
 * @param <S> state enum type
 *
 * @author Xylonity
 */
public interface StatefulEntity<E extends Mob, S extends Enum<S>> {

    AutomatonHandler<E, S> getAutomatonHandler();

    /**
     * Always {@code null} on the client, as the AI only runs server side
     */
    @Nullable
    default Automaton<E, S> getAutomaton() {
        return getAutomatonHandler().getAutomaton();
    }

    default S getCurrentState() {
        return getAutomatonHandler().getCurrentState();
    }

}
