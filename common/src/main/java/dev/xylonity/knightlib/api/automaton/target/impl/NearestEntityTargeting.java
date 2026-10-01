package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.TargetSelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Targets the nearest living entity of a given class within a radius.
 *
 * @param <E> the host entity type
 * @param <T> the candidate class scanned for
 *
 * @author Xylonity
 */
public class NearestEntityTargeting<E extends Mob, T extends LivingEntity> implements TargetSelector<E> {

    private final Class<T> candidateClass;
    private final double radius;
    private final int retargetInterval;
    private final TargetingConditions conditions;

    private int tickCounter;

    @Nullable
    private T selected;

    public NearestEntityTargeting(Class<T> candidateClass, double radius) {
        this(candidateClass, radius, 10, false, null);
    }

    public NearestEntityTargeting(Class<T> candidateClass, double radius, int retargetInterval, boolean requireLineOfSight, @Nullable Predicate<T> filter) {
        this.candidateClass = candidateClass;
        this.radius = radius;
        this.retargetInterval = Math.max(1, retargetInterval);
        this.conditions = TargetingConditions.forCombat().range(radius);

        if (filter != null) {
            this.conditions.selector(living -> filter.test(candidateClass.cast(living)));
        }

        if (!requireLineOfSight) {
            this.conditions.ignoreLineOfSight();
        }

    }

    @Override
    public void onStart(E entity) {
        tickCounter = 0;
        selected = null;
    }

    @Nullable
    @Override
    public LivingEntity select(E entity, @Nullable LivingEntity current) {
        if (tickCounter++ % retargetInterval == 0) {
            // Keeps the current target if it still qualifies
            if (candidateClass.isInstance(current) && conditions.test(entity, current)) {
                selected = candidateClass.cast(current);
            }
            else {
                selected = entity.level().getNearestEntity(candidateClass, conditions, entity, entity.getX(), entity.getY(), entity.getZ(), entity.getBoundingBox().inflate(radius));
            }

        }
        else if (selected != null && !selected.isAlive()) {
            selected = null;
        }

        return selected;
    }

}
