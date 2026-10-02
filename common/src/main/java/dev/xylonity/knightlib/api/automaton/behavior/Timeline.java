package dev.xylonity.knightlib.api.automaton.behavior;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Tick based command driven by {@link BehaviorContext#ticksInState()}, where the first tick in the state is 1. It can be
 * registered as the behavior itself or called from another behavior's tick
 *
 * <pre>{@code
 * private static final Timeline<Boss, CakeIsALieState> SWING = Timeline.<Boss, CakeIsALieState>builder()
 *         .during(1, 12, (boss, context) -> boss.chaseTarget())
 *         .at(9, (boss, context) -> boss.glow())
 *         .at(13, (boss, context) -> boss.hitInFront())
 *         .endAt(25, CakeIsALieState.CHASE)
 *         .build();
 * }</pre>
 *
 * @param <E> entity type
 * @param <S> state enum type
 *
 * @author Xylonity
 */
public final class Timeline<E, S extends Enum<S>> implements Behavior<E, S> {

    private final List<Span<E, S>> spans;
    private final int endTick;

    @Nullable
    private final S endState;

    private Timeline(List<Span<E, S>> spans, int endTick, @Nullable S endState) {
        this.spans = spans;
        this.endTick = endTick;
        this.endState = endState;
    }

    public static <E, S extends Enum<S>> Builder<E, S> builder() {
        return new Builder<>();
    }

    @Nullable
    @Override
    public S tick(E entity, BehaviorContext<S> context) {
        final int tick = context.ticksInState();
        for (final Span<E, S> span : spans) {
            if (tick >= span.from && tick <= span.to) {
                span.action.accept(entity, context);
            }

        }

        return endState != null && tick >= endTick ? endState : null;
    }

    private record Span<E, S extends Enum<S>>(
            int from,
            int to,
            BiConsumer<E, BehaviorContext<S>> action
    ) {
        ;;
    }

    public static final class Builder<E, S extends Enum<S>> {

        private final List<Span<E, S>> spans = new ArrayList<>();
        private int endTick;

        @Nullable
        private S endState;

        private Builder() {
            ;;
        }

        /**
         * Runs the action once, on the given tick
         */
        public Builder<E, S> at(int tick, BiConsumer<E, BehaviorContext<S>> action) {
            return during(tick, tick, action);
        }

        /**
         * Runs the action every tick between both ticks
         */
        public Builder<E, S> during(int fromTick, int toTick, BiConsumer<E, BehaviorContext<S>> action) {
            spans.add(new Span<>(fromTick, toTick, action));
            return this;
        }

        /**
         * Transitions to the given state once the tick is reached, or the timeline won't end
         */
        public Builder<E, S> endAt(int tick, S state) {
            this.endTick = tick;
            this.endState = state;
            return this;
        }

        public Timeline<E, S> build() {
            return new Timeline<>(List.copyOf(spans), endTick, endState);
        }

    }

}
