package dev.xylonity.knightlib.api.automaton;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Per entity link between a {@link Mob} and its {@link Automaton}. Builds the automaton on the server, syncs the current
 * state to clients and forwards the vanilla hooks to it. See {@link StatefulEntity}
 *
 * @param <E> entity type
 * @param <S> state enum type
 *
 * @author Xylonity
 */
public final class AutomatonHandler<E extends Mob, S extends Enum<S>> {

    private final E entity;
    private final EntityDataAccessor<Integer> stateAccessor;
    private final Supplier<Automaton<E, S>> factory;
    private final S defaultState;
    private final S[] states;

    @Nullable
    private Automaton<E, S> automaton;

    private AutomatonHandler(E entity, EntityDataAccessor<Integer> stateAccessor, Supplier<Automaton<E, S>> factory, S defaultState) {
        this.entity = entity;
        this.stateAccessor = stateAccessor;
        this.factory = factory;
        this.defaultState = defaultState;
        this.states = defaultState.getDeclaringClass().getEnumConstants();
    }

    /**
     * @param stateAccessor synced integer holding the current state, defined by the entity with the default state's ordinal
     */
    public static <E extends Mob, S extends Enum<S>> AutomatonHandler<E, S> of(E entity, EntityDataAccessor<Integer> stateAccessor, Supplier<Automaton<E, S>> factory, S defaultState) {
        return new AutomatonHandler<>(entity, stateAccessor, factory, defaultState);
    }

    /**
     * Returns the automaton
     */
    @Nullable
    public Automaton<E, S> getAutomaton() {
        if (automaton == null && !entity.level().isClientSide) {
            automaton = factory.get();
        }

        return automaton;
    }

    public S getCurrentState() {
        final int ordinal = entity.getEntityData().get(stateAccessor);
        return ordinal >= 0 && ordinal < states.length ? states[ordinal] : defaultState;
    }

    /**
     * Syncs the current state to clients, must be called every tick
     */
    public void tick() {
        if (getAutomaton() != null) {
            entity.getEntityData().set(stateAccessor, automaton.currentState().ordinal());
        }

    }

    public void onHurt(DamageSource source, float amount) {
        if (automaton != null) {
            automaton.onDamaged(entity, source, amount);
        }

    }

    public void onDeath() {
        if (automaton != null) {
            automaton.onDeath(entity);
        }

    }

    public void onTargetChanged(@Nullable LivingEntity previous, @Nullable LivingEntity target) {
        if (automaton != null) {
            automaton.onTargetChanged(entity, previous, target);
        }

    }

    public void onEffectAdded(MobEffectInstance effectInstance) {
        if (automaton != null) {
            automaton.onEffectAdded(entity, effectInstance);
        }

    }

}
