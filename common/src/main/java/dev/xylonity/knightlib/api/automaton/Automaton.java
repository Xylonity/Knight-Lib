package dev.xylonity.knightlib.api.automaton;

import dev.xylonity.knightlib.api.automaton.behavior.Behavior;
import dev.xylonity.knightlib.api.automaton.behavior.BehaviorContext;
import dev.xylonity.knightlib.api.automaton.target.Targeting;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Generic finite-state machine (state automaton) for entity AI.
 *
 * <p>Maps state ids (from {@link StateEnum}) to {@link Behavior} instances. Each tick the active
 * behavior is updated and may request a transition by returning a target state id. Transitions
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
 * @param <S> state enum type (must implement {@link StateEnum})
 *
 * @author Xylonity
 */
public class Automaton<E, S extends Enum<S> & StateEnum> {

    private final Int2ObjectMap<Behavior<E, S>> behaviors;
    private final Map<Integer, IntSet> blockedTransitions;
    private final List<GlobalRule<E>> globalRules;
    private final S[] stateValues;
    private final BehaviorContext context = new BehaviorContext();
    private final Queue<StateTransition> transitionQueue = new LinkedList<>();

    private Behavior<E, S> currentBehavior;

    private int currentStateId;
    private int ticksInState;
    private boolean firstTick;
    private float lastDamageAmount;

    @Nullable
    private Targeting<E> targeting;

    private Automaton(Builder<E, S> builder) {
        this.behaviors = builder.behaviors;
        this.blockedTransitions = builder.blockedTransitions;
        this.globalRules = builder.globalRules;
        this.targeting = builder.targeting;
        this.currentStateId = builder.initialState.id();
        this.stateValues = builder.initialState.getDeclaringClass().getEnumConstants();
        this.ticksInState = 0;
        this.firstTick = true;
    }

    public static <E, S extends Enum<S> & StateEnum> Builder<E, S> builder(S initialState) {
        return new Builder<>(initialState);
    }

    /**
     * Initializes the automaton in its initial state
     */
    public void start(E entity) {
        currentBehavior = behaviors.get(currentStateId);
        if (currentBehavior == null) {
            throw new IllegalStateException(
                    String.format("[KnightLib] No behavior registered for initial state id %d. Registered: %s", currentStateId, behaviors.keySet())
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
        final Integer forcedExit = currentBehavior.shouldForceExit(entity, context);
        if (forcedExit != null) {
            transitionTo(entity, forcedExit, false);
            return;
        }

        // Global rules (evaluated before the behavior tick)
        for (GlobalRule<E> rule : globalRules) {
            final Integer ruleTarget = rule.evaluate(entity, context, currentStateId);
            if (ruleTarget != null && ruleTarget != currentStateId) {
                transitionQueue.add(new StateTransition(ruleTarget, true, TransitionPriority.HIGH));
                break;
            }

        }

        if (firstTick) {
            currentBehavior.onFirstTick(entity, context);
            firstTick = false;
        }

        final Integer nextStateId = currentBehavior.tick(entity, context);

        currentBehavior.onTickEnd(entity, context);

        // Direct transition requested by the behavior tick
        if (nextStateId != null && nextStateId != currentStateId) {
            transitionTo(entity, nextStateId, false);
        }

        processTransitionQueue(entity);
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

        final Integer nextState = currentBehavior.onDamaged(entity, context, amount);
        if (nextState != null && nextState != currentStateId) {
            transitionQueue.add(new StateTransition(nextState, true, TransitionPriority.HIGH));
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
     * Enqueues a transition request with a given priority.
     */
    public void requestTransition(E entity, S targetState, TransitionPriority priority) {
        transitionQueue.add(new StateTransition(targetState.id(), true, priority));
    }

    /**
     * Sorts and processes the queued transitions by priority (highest first).
     * The first transition that successfully applied wins, and the rest are discarded for this tick
     */
    private void processTransitionQueue(E entity) {
        if (transitionQueue.isEmpty()) {
            return;
        }

        final List<StateTransition> sorted = new ArrayList<>(transitionQueue);
        sorted.sort(Comparator.comparingInt(stateTransition -> -stateTransition.priority.value));
        transitionQueue.clear();

        for (final StateTransition transition : sorted) {
            if (attemptTransition(entity, transition)) {
                break;
            }

        }

    }

    /**
     * Attempts to apply a queued transition, respecting the interruption rules applied
     */
    private boolean attemptTransition(E entity, StateTransition transition) {
        if (transition.isInterrupt && !currentBehavior.canBeInterrupted(entity, context, transition.targetState)) {
            return false;
        }

        transitionTo(entity, transition.targetState, transition.isInterrupt);

        return true;
    }

    /**
     * Performs an internal state transition and runs lifecycle hooks
     */
    private void transitionTo(E entity, int targetStateId, boolean isInterrupt) {
        final Behavior<E, S> targetBehavior = behaviors.get(targetStateId);
        if (targetBehavior == null) {
            return;
        }

        if (isTransitionBlocked(currentStateId, targetStateId)) {
            return;
        }

        context.setPreviousState(currentStateId);
        if (!targetBehavior.canStart(entity, context)) {
            return;
        }

        // Exits the current behavior
        context.setInterrupted(isInterrupt);
        currentBehavior.onExit(entity, context, isInterrupt);

        final int previousStateId = currentStateId;

        // Switches state
        currentStateId = targetStateId;
        currentBehavior = targetBehavior;

        // Resets per-state bookkeeping
        ticksInState = 0;
        firstTick = true;

        context.clearTransient();

        context.setTicksInState(ticksInState);
        context.setPreviousState(previousStateId);

        // The new state starts non-interrupted, as it applies to the exit transition
        context.setInterrupted(false);

        currentBehavior.onEnter(entity, context);
    }

    /**
     * Returns {@code true} if this specific transition has been explicitly blocked
     */
    private boolean isTransitionBlocked(int fromState, int toState) {
        final IntSet blocked = blockedTransitions.get(fromState);
        return blocked != null && blocked.contains(toState);
    }

    /**
     * Resolves an integer state id back to the typed enum constant
     */
    @Nullable
    public S resolveState(int stateId) {
        for (S state : stateValues) {
            if (state.id() == stateId) {
                return state;
            }

        }

        return null;
    }

    public int currentStateId() {
        return currentStateId;
    }

    @Nullable
    public S currentState() {
        return resolveState(currentStateId);
    }

    public int ticksInState() {
        return ticksInState;
    }

    public BehaviorContext context() {
        return context;
    }

    public boolean isInState(S state) {
        return currentStateId == state.id();
    }

    public float getLastDamageAmount() {
        return lastDamageAmount;
    }

    /**
     * Represents a transition request (usually enqueued by events)
     */
    private record StateTransition(
            int targetState,
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

    public static class Builder<E, S extends Enum<S> & StateEnum> {

        private final S initialState;

        private final Int2ObjectMap<Behavior<E, S>> behaviors = new Int2ObjectOpenHashMap<>();
        private final Map<Integer, IntSet> blockedTransitions = new HashMap<>();
        private final List<GlobalRule<E>> globalRules = new ArrayList<>();

        @Nullable
        private Targeting<E> targeting;

        private Builder(S initialState) {
            this.initialState = initialState;
        }

        /**
         * Registers a behavior for a given state
         */
        public Builder<E, S> register(S state, Behavior<E, S> behavior) {
            behaviors.put(state.id(), behavior);
            return this;
        }

        /**
         * Blocks a single directed transition, silently ignoring any attempt to go from {@code state} to {@code targetState}
         */
        public Builder<E, S> blockTransition(S state, S targetState) {
            blockedTransitions.computeIfAbsent(state.id(), integer -> new IntOpenHashSet()).add(targetState.id());
            return this;
        }

        /**
         * Blocks multiple directed transitions from one state
         */
        @SafeVarargs
        public final Builder<E, S> blockTransitions(S from, S... blocked) {
            final IntSet set = blockedTransitions.computeIfAbsent(from.id(), integer -> new IntOpenHashSet());
            for (final S state : blocked) {
                set.add(state.id());
            }

            return this;
        }

        /**
         * Adds a global rule evaluated every tick before the active behavior
         */
        public Builder<E, S> globalRule(GlobalRule<E> rule) {
            globalRules.add(rule);
            return this;
        }

        /**
         * Sets the targeting system
         */
        public Builder<E, S> targeting(@Nullable Targeting<E> targeting) {
            this.targeting = targeting;
            return this;
        }

        /**
         * Builds the automaton.
         *
         * @throws IllegalStateException if the initial state has no registered behavior
         */
        public Automaton<E, S> build() {
            if (!behaviors.containsKey(initialState.id())) {
                throw new IllegalStateException(
                        String.format("[KnightLib] Initial state '%s' (id=%d) has no registered behavior", initialState.name(), initialState.id())
                );

            }

            return new Automaton<>(this);
        }

    }

}