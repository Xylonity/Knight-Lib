package dev.xylonity.knightlib.api.automaton;

import dev.xylonity.knightlib.api.automaton.behavior.Behavior;
import dev.xylonity.knightlib.api.automaton.behavior.BehaviorContext;
import dev.xylonity.knightlib.api.automaton.target.Targeting;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Generic finite-state machine (state automaton) for entity AI.
 *
 * <p>Maps states to {@link Behavior} instances. Each tick the active
 * behavior is updated and may request a transition by returning a target state. Transitions
 * can also be requested asynchronously via a priority queue ({@link #requestTransition}).</p>
 *
 * <h4>Transition model</h4>
 * <p>By default, all transitions are allowed. Use {@link Builder#blockTransition} to
 * blacklist specific pairs that should never happen.</p>
 *
 * <h4>Global rules</h4>
 * <p>{@link GlobalRule}s are evaluated every tick <b>before</b> the active behavior. They let you
 * express cross-state conditions ("explode below 20% HP" for example) once instead of duplicating the check
 * in every behavior.</p>
 *
 * <p>Use {@link #builder(Enum)} to construct instances.</p>
 *
 * @param <E> entity type
 * @param <S> state enum type
 *
 * @author Xylonity
 */
public class Automaton<E, S extends Enum<S>> {

    private final Map<S, Behavior<E, S>> behaviors;
    private final Map<S, Set<S>> blockedTransitions;
    private final List<GlobalRule<E, S>> globalRules;
    private final BehaviorContext<S> context = new BehaviorContext<>();
    private final Queue<StateTransition<S>> transitionQueue = new LinkedList<>();

    private Behavior<E, S> currentBehavior;

    private S currentState;
    private int ticksInState;
    private boolean firstTick;
    private float lastDamageAmount;

    @Nullable
    private final Targeting<? super E> targeting;

    private Automaton(Builder<E, S> builder) {
        this.behaviors = builder.behaviors;
        this.blockedTransitions = builder.blockedTransitions;
        this.globalRules = builder.globalRules;
        this.targeting = builder.targeting;
        this.currentState = builder.initialState;
        this.ticksInState = 0;
        this.firstTick = true;
    }

    public static <E, S extends Enum<S>> Builder<E, S> builder(S initialState) {
        return new Builder<>(initialState);
    }

    /**
     * Initializes the automaton in its initial state
     */
    public void start(E entity) {
        currentBehavior = behaviors.get(currentState);
        if (currentBehavior == null) {
            throw new IllegalStateException(
                    String.format("[KnightLib] No behavior registered for initial state %s. Registered: %s", currentState, behaviors.keySet())
            );

        }

        ticksInState = 0;
        firstTick = true;
        context.clearTransient();
        context.setTicksInState(ticksInState);
        context.setInterrupted(false);

        if (targeting != null) {
            targeting.onStart(entity);
        }

        currentBehavior.onEnter(entity, context);
    }

    /**
     * Stops the automaton and exits the current working behavior
     */
    public void stop(E entity) {
        if (currentBehavior != null) {
            currentBehavior.onExit(entity, context, false);
        }

        if (targeting != null) {
            targeting.onStop(entity);
        }

    }

    /**
     * Ticks the current automaton (this method, along with {@link #start} and {@link #stop} must be called for the automaton to work). The helper
     * goal class {@link dev.xylonity.knightlib.api.automaton.goal.StateMachineGoal} does this job automatically, which is embed inside
     * {@link dev.xylonity.knightlib.common.entity.StatefulCompanionEntity} and {@link dev.xylonity.knightlib.common.entity.StatefulHostileEntity}.
     */
    public void tick(E entity) {
        if (currentBehavior == null) {
            return;
        }

        if (targeting != null) {
            targeting.tick(entity);
        }

        ticksInState++;
        context.setTicksInState(ticksInState);

        // Forces exit check during the current behavior
        final S forcedExit = currentBehavior.shouldForceExit(entity, context);
        if (forcedExit != null && transitionTo(entity, forcedExit, false)) {
            return;
        }

        // Global rules (evaluated before the behavior tick)
        for (GlobalRule<E, S> rule : globalRules) {
            final S ruleTarget = rule.evaluate(entity, context, currentState);
            if (ruleTarget != null && ruleTarget != currentState) {
                transitionQueue.add(new StateTransition<>(ruleTarget, true, TransitionPriority.HIGH));
                break;
            }

        }

        if (processTransitionQueue(entity)) {
            return;
        }

        if (firstTick) {
            currentBehavior.onFirstTick(entity, context);
            firstTick = false;
        }

        final S nextState = currentBehavior.tick(entity, context);

        currentBehavior.onTickEnd(entity, context);

        // Direct transition requested by the behavior tick
        if (nextState != null) {
            transitionTo(entity, nextState, false);
        }

    }

    /**
     * Notifies the automaton that the entity in this context took damage
     */
    public void onDamaged(E entity, @Nullable DamageSource source, float amount) {
        if (currentBehavior == null) {
            return;
        }

        lastDamageAmount = amount;

        if (targeting != null) {
            targeting.onDamaged(entity, source, amount);
        }

        final S nextState = currentBehavior.onDamaged(entity, context, amount);
        if (nextState != null && nextState != currentState) {
            transitionQueue.add(new StateTransition<>(nextState, true, TransitionPriority.HIGH));
        }

    }

    /**
     * Notifies the automaton that the entity in this context died
     */
    public void onDeath(E entity) {
        if (currentBehavior != null) {
            currentBehavior.onDeath(entity, context);
        }

    }

    /**
     * Notifies the automaton that the target of the entity in this context has changed
     */
    public void onTargetChanged(E entity, @Nullable LivingEntity previousTarget, @Nullable LivingEntity newTarget) {
        if (currentBehavior != null) {
            currentBehavior.onTargetChanged(entity, context, previousTarget, newTarget);
        }

    }

    /**
     * Notifies the automaton that an effect has been applied to the entity in this context
     */
    public void onEffectAdded(E entity, @Nullable MobEffectInstance effectInstance) {
        if (currentBehavior != null) {
            currentBehavior.onEffectAdded(entity, context, effectInstance);
        }

    }

    /**
     * Enqueues a transition request with a given priority
     */
    public void requestTransition(E entity, S targetState, TransitionPriority priority) {
        if (targetState != currentState) {
            transitionQueue.add(new StateTransition<>(targetState, true, priority));
        }

    }

    /**
     * Sorts and processes the queued transitions by priority (highest first).
     * The first transition that successfully applied wins, and the rest are discarded for this tick
     *
     * @return {@code true} if a transition was applied
     */
    private boolean processTransitionQueue(E entity) {
        if (transitionQueue.isEmpty()) {
            return false;
        }

        final List<StateTransition<S>> sorted = new ArrayList<>(transitionQueue);
        sorted.sort(Comparator.comparingInt(stateTransition -> -stateTransition.priority.value));
        transitionQueue.clear();

        for (final StateTransition<S> transition : sorted) {
            if (attemptTransition(entity, transition)) {
                return true;
            }

        }

        return false;
    }

    /**
     * Attempts to apply a queued transition, respecting the interruption rules applied
     */
    private boolean attemptTransition(E entity, StateTransition<S> transition) {
        if (transition.isInterrupt && !currentBehavior.canBeInterrupted(entity, context, transition.targetState)) {
            return false;
        }

        return transitionTo(entity, transition.targetState, transition.isInterrupt);
    }

    /**
     * Performs an internal state transition and runs lifecycle hooks
     *
     * @return {@code true} if the transition was applied
     */
    private boolean transitionTo(E entity, S targetState, boolean isInterrupt) {
        if (targetState == currentState) {
            return false;
        }

        final Behavior<E, S> targetBehavior = behaviors.get(targetState);
        if (targetBehavior == null) {
            return false;
        }

        if (isTransitionBlocked(currentState, targetState)) {
            return false;
        }

        // canStart sees the state it would come from as the previous one, restored if the transition is rejected
        final S lastPreviousState = context.previousState();
        context.setPreviousState(currentState);
        if (!targetBehavior.canStart(entity, context)) {
            context.setPreviousState(lastPreviousState);
            return false;
        }

        // Exits the current behavior
        context.setInterrupted(isInterrupt);
        currentBehavior.onExit(entity, context, isInterrupt);

        final S previousState = currentState;

        // Switches state
        currentState = targetState;
        currentBehavior = targetBehavior;

        // Resets per-state bookkeeping
        ticksInState = 0;
        firstTick = true;

        // Pending requests were made against the previous state (including any made from its onExit)
        transitionQueue.clear();
        context.clearTransient();

        context.setTicksInState(ticksInState);
        context.setPreviousState(previousState);

        // The new state starts non-interrupted, as it applies to the exit transition
        context.setInterrupted(false);

        currentBehavior.onEnter(entity, context);

        return true;
    }

    /**
     * Returns {@code true} if this specific transition has been explicitly blocked
     */
    private boolean isTransitionBlocked(S fromState, S toState) {
        final Set<S> blocked = blockedTransitions.get(fromState);
        return blocked != null && blocked.contains(toState);
    }

    public S currentState() {
        return currentState;
    }

    public int ticksInState() {
        return ticksInState;
    }

    public BehaviorContext<S> context() {
        return context;
    }

    public boolean isInState(S state) {
        return currentState == state;
    }

    public float getLastDamageAmount() {
        return lastDamageAmount;
    }

    /**
     * Represents a transition request (usually enqueued by events)
     */
    private record StateTransition<S>(
            S targetState,
            boolean isInterrupt,
            TransitionPriority priority
    ) {
        ;;
    }

    public enum TransitionPriority {
        LOW(0),
        NORMAL(1),
        HIGH(2),
        CRITICAL(3);

        final int value;

        TransitionPriority(int value) {
            this.value = value;
        }

    }

    public static class Builder<E, S extends Enum<S>> {

        private final S initialState;

        private final Map<S, Behavior<E, S>> behaviors;
        private final Map<S, Set<S>> blockedTransitions;
        private final List<GlobalRule<E, S>> globalRules = new ArrayList<>();

        @Nullable
        private Targeting<? super E> targeting;

        private Builder(S initialState) {
            this.initialState = initialState;
            this.behaviors = new EnumMap<>(initialState.getDeclaringClass());
            this.blockedTransitions = new EnumMap<>(initialState.getDeclaringClass());
        }

        /**
         * Registers a behavior for a given state
         */
        public Builder<E, S> register(S state, Behavior<E, S> behavior) {
            behaviors.put(state, behavior);
            return this;
        }

        /**
         * Blocks a single directed transition, silently ignoring any attempt to go from {@code state} to {@code targetState}
         */
        public Builder<E, S> blockTransition(S state, S targetState) {
            blockedTransitions.computeIfAbsent(state, s -> EnumSet.noneOf(s.getDeclaringClass())).add(targetState);
            return this;
        }

        /**
         * Blocks multiple directed transitions from one state
         */
        @SafeVarargs
        public final Builder<E, S> blockTransitions(S from, S... blocked) {
            final Set<S> set = blockedTransitions.computeIfAbsent(from, s -> EnumSet.noneOf(s.getDeclaringClass()));
            set.addAll(Arrays.asList(blocked));

            return this;
        }

        /**
         * Adds a global rule evaluated every tick before the active behavior
         */
        public Builder<E, S> globalRule(GlobalRule<E, S> rule) {
            globalRules.add(rule);
            return this;
        }

        /**
         * Sets the targeting system
         */
        public Builder<E, S> targeting(@Nullable Targeting<? super E> targeting) {
            this.targeting = targeting;
            return this;
        }

        /**
         * Builds the automaton.
         *
         * @throws IllegalStateException if the initial state has no registered behavior
         */
        public Automaton<E, S> build() {
            if (!behaviors.containsKey(initialState)) {
                throw new IllegalStateException(
                        String.format("[KnightLib] Initial state '%s' has no registered behavior", initialState.name())
                );

            }

            return new Automaton<>(this);
        }

    }

}