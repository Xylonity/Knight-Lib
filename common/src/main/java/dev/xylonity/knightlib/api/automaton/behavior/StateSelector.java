package dev.xylonity.knightlib.api.automaton.behavior;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleBiFunction;

/**
 * Random choice between states, typically the attacks a behavior can go into. Weights are evaluated on every selection, so returning 0 leaves that option out
 *
 * <pre>{@code
 * private static final StateSelector<Boss, BossState> ATTACKS = StateSelector.<Boss, BossState>builder()
 *         .option(BossState.SLAM, (boss, context) -> SLAM_COOLDOWN.isReady(boss, context) ? 3 : 0)
 *         .option(BossState.LIGHTNING, 1)
 *         .none(2)
 *         .build();
 * }</pre>
 *
 * @param <E> entity type
 * @param <S> state enum type
 *
 * @author Xylonity
 */
public final class StateSelector<E extends LivingEntity, S extends Enum<S>> {

    private final List<Option<E, S>> options;
    private final double noneWeight;

    private StateSelector(List<Option<E, S>> options, double noneWeight) {
        this.options = options;
        this.noneWeight = noneWeight;
    }

    public static <E extends LivingEntity, S extends Enum<S>> Builder<E, S> builder() {
        return new Builder<>();
    }

    @Nullable
    public S select(E entity, BehaviorContext<S> context) {
        return select(entity, context, entity.getRandom());
    }

    /**
     * Picks a state proportionally to its weight or {@code null} if nothing is picked
     */
    @Nullable
    public S select(E entity, BehaviorContext<S> context, RandomSource random) {
        final double[] weights = new double[options.size()];
        double total = noneWeight;

        for (int i = 0; i < weights.length; i++) {
            weights[i] = Math.max(0, options.get(i).weight.applyAsDouble(entity, context));
            total += weights[i];
        }

        if (total <= 0) {
            return null;
        }

        double roll = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return options.get(i).state;
            }

        }

        return null;
    }

    private record Option<E, S extends Enum<S>>(
            S state,
            ToDoubleBiFunction<E, BehaviorContext<S>> weight
    ) {
        ;;
    }

    public static final class Builder<E extends LivingEntity, S extends Enum<S>> {

        private final List<Option<E, S>> options = new ArrayList<>();
        private double noneWeight;

        private Builder() {
            ;;
        }

        public Builder<E, S> option(S state, double weight) {
            return option(state, (entity, context) -> weight);
        }

        public Builder<E, S> option(S state, ToDoubleBiFunction<E, BehaviorContext<S>> weight) {
            options.add(new Option<>(state, weight));
            return this;
        }

        /**
         * Weight of picking nothing
         */
        public Builder<E, S> none(double weight) {
            this.noneWeight = Math.max(0, weight);
            return this;
        }

        public StateSelector<E, S> build() {
            return new StateSelector<>(List.copyOf(options), noneWeight);
        }

    }

}