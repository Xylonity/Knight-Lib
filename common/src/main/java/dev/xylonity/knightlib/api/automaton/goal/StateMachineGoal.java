package dev.xylonity.knightlib.api.automaton.goal;

import dev.xylonity.knightlib.api.automaton.Automaton;
import dev.xylonity.knightlib.api.automaton.StateEnum;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Bridges an {@link Automaton} into vanilla's {@link Goal} system.
 *
 * @param <E> entity type
 * @param <S> state enum type
 */
public class StateMachineGoal<E, S extends Enum<S> & StateEnum> extends Goal {

    private final E entity;
    private final Automaton<E, S> automaton;

    public StateMachineGoal(E entity, Automaton<E, S> automaton) {
        this.entity = entity;
        this.automaton = automaton;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start() {
        automaton.start(entity);
    }

    @Override
    public void stop() {
        automaton.stop(entity);
    }

    @Override
    public void tick() {
        automaton.tick(entity);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

}