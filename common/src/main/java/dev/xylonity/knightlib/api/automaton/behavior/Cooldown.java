package dev.xylonity.knightlib.api.automaton.behavior;

import net.minecraft.world.entity.Entity;

/**
 * Game time based cooldown stored in the {@link BehaviorContext}, so it survives state transitions and can be shared
 * between behaviors. Declare it as a constant, check it with {@link #isReady} and {@link #trigger} it when used
 *
 * @author Xylonity
 */
public final class Cooldown {

    private final BehaviorData<Long> readyAt;
    private final int durationTicks;

    private Cooldown(String key, int durationTicks) {
        this.readyAt = BehaviorData.longKey("cooldown/" + key);
        this.durationTicks = durationTicks;
    }

    public static Cooldown of(String key, int durationTicks) {
        return new Cooldown(key, durationTicks);
    }

    public boolean isReady(Entity entity, BehaviorContext<?> context) {
        return remainingTicks(entity, context) == 0;
    }

    public long remainingTicks(Entity entity, BehaviorContext<?> context) {
        return Math.max(0, context.get(readyAt) - entity.level().getGameTime());
    }

    public void trigger(Entity entity, BehaviorContext<?> context) {
        trigger(entity, context, durationTicks);
    }

    public void trigger(Entity entity, BehaviorContext<?> context, int durationTicks) {
        context.set(readyAt, entity.level().getGameTime() + durationTicks);
    }

    public void reset(BehaviorContext<?> context) {
        context.remove(readyAt);
    }

}
