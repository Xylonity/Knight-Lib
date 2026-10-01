package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.Targeting;
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
public class NearestEntityTargeting<E extends LivingEntity, T extends LivingEntity> implements Targeting<E> {

    private final Class<T> candidateClass;
    private final double radius;
    private final int retargetInterval;
    private final TargetingConditions conditions;

    private int tickCounter;

    @Nullable
    private T acquired;

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
        acquired = null;
    }

    @Override
    public void tick(E entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (tickCounter++ % retargetInterval != 0) {
            return;
        }

        final T current = currentOfType(entity);

        // Something else retargeted the host, so this instance no longer owns the target
        if (current != acquired) {
            acquired = null;
        }

        // Keeps the current target if it still qualifies
        if (current != null && conditions.test(entity, current)) {
            return;
        }

        final T nearest = entity.level().getNearestEntity(candidateClass, conditions, entity, entity.getX(), entity.getY(), entity.getZ(), entity.getBoundingBox().inflate(radius));
        if (nearest != null) {
            setTarget(entity, nearest);
            acquired = nearest;
        }
        else if (acquired != null) {
            setTarget(entity, null);
            acquired = null;
        }

    }

    protected void setTarget(E entity, @Nullable LivingEntity target) {
        if (entity instanceof Mob mob) {
            mob.setTarget(target);
        }

    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected T currentOfType(E entity) {
        if (!(entity instanceof Mob mob)) {
            return null;
        }

        final LivingEntity target = mob.getTarget();
        if (target == null) {
            return null;
        }

        return candidateClass.isInstance(target) ? (T) target : null;
    }

}