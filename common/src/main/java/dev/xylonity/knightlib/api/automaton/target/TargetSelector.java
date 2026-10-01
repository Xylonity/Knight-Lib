package dev.xylonity.knightlib.api.automaton.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

/**
 * {@link Targeting} that proposes a target instead of setting it, so several of them can be combined
 * with {@link dev.xylonity.knightlib.api.automaton.target.impl.CompositeTargeting}.
 *
 * The proposed target replaces the host's current one, and {@code null} clears it
 *
 * @param <E> the host entity type
 *
 * @author Xylonity
 */
@FunctionalInterface
public interface TargetSelector<E extends Mob> extends Targeting<E> {

    /**
     * Called every tick, returns the target the host should have
     */
    @Nullable
    LivingEntity select(E entity, @Nullable LivingEntity current);

    @Override
    default void tick(E entity) {
        final LivingEntity current = entity.getTarget();
        final LivingEntity selected = select(entity, current);
        if (selected != current) {
            entity.setTarget(selected);
        }

    }

}
