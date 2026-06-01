package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.Targeting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
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
    private final boolean requireLineOfSight;
    private final Predicate<T> filter;

    private int tickCounter;

    public NearestEntityTargeting(Class<T> candidateClass, double radius) {
        this(candidateClass, radius, 10, false, null);
    }

    public NearestEntityTargeting(Class<T> candidateClass, double radius, int retargetInterval, boolean requireLineOfSight, @Nullable Predicate<T> filter) {
        this.candidateClass = candidateClass;
        this.radius = radius;
        this.retargetInterval = Math.max(1, retargetInterval);
        this.requireLineOfSight = requireLineOfSight;
        this.filter = filter != null ? filter : t -> true;
    }

    @Override
    public void onStart(E entity) {
        tickCounter = 0;
    }

    @Override
    public void tick(E entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (tickCounter++ % retargetInterval != 0) {
            return;
        }

        // Keeps the current target if it still qualifies
        final T current = currentOfType(entity);
        if (current != null && current.isAlive() && entity.distanceTo(current) <= radius && isValidCandidate(entity, current)) {
            return;
        }

        final T nearest = findNearest(entity);
        if (nearest != null) {
            setTarget(entity, nearest);
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

    @Nullable
    private T findNearest(E entity) {
        final AABB box = entity.getBoundingBox().inflate(radius);
        final List<T> candidates = entity.level().getEntitiesOfClass(candidateClass, box, t -> isValidCandidate(entity, t));

        T nearest = null;
        double nearestDistanceSquare = radius * radius;
        for (final T candidate : candidates) {
            final double distanceSquare = entity.distanceToSqr(candidate);
            if (distanceSquare < nearestDistanceSquare) {
                nearestDistanceSquare = distanceSquare;
                nearest = candidate;
            }

        }

        return nearest;
    }

    private boolean isValidCandidate(E entity, T candidate) {
        if (candidate == entity || !candidate.isAlive()) {
            return false;
        }

        if (!filter.test(candidate)) {
            return false;
        }

        if (requireLineOfSight && entity instanceof Mob mob && !mob.getSensing().hasLineOfSight(candidate)) {
            return false;
        }

        return true;
    }

}