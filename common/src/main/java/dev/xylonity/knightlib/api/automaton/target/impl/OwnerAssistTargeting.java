package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.TargetSelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Companion targeting that forwards the owner's combat state onto the host.
 *
 * Ownership is resolved via the supplied {@link Function}, decoupling this class from any specific taming system
 *
 * @param <E> the host entity type
 *
 * @author Xylonity
 */
public class OwnerAssistTargeting<E extends Mob> implements TargetSelector<E> {

    private static final TargetingConditions CONDITIONS = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private final Function<E, LivingEntity> ownerResolver;
    private final int retargetInterval;

    private int tickCounter;

    @Nullable
    private LivingEntity assisted;

    public OwnerAssistTargeting(Function<E, LivingEntity> ownerResolver) {
        this(ownerResolver, 5);
    }

    public OwnerAssistTargeting(Function<E, LivingEntity> ownerResolver, int retargetInterval) {
        this.ownerResolver = ownerResolver;
        this.retargetInterval = Math.max(1, retargetInterval);
    }

    @Override
    public void onStart(E entity) {
        tickCounter = 0;
        assisted = null;
    }

    @Nullable
    @Override
    public LivingEntity select(E entity, @Nullable LivingEntity current) {
        if (tickCounter++ % retargetInterval == 0) {
            final LivingEntity candidate = findCandidate(entity);
            if (candidate != null) {
                assisted = candidate;
            }

        }

        if (assisted != null && !CONDITIONS.test(entity, assisted)) {
            assisted = null;
        }

        return assisted;
    }

    @Nullable
    private LivingEntity findCandidate(E entity) {
        final LivingEntity owner = ownerResolver.apply(entity);
        if (owner == null || !owner.isAlive()) {
            return null;
        }

        // The owner is already attacking something, so the host joins in
        if (owner instanceof Mob ownerMob && isValidCandidate(entity, owner, ownerMob.getTarget())) {
            return ownerMob.getTarget();
        }

        // Otherwise the host retaliates against whoever last hit the owner
        final LivingEntity hurtBy = owner.getLastHurtByMob();
        return isValidCandidate(entity, owner, hurtBy) ? hurtBy : null;
    }

    private boolean isValidCandidate(E entity, LivingEntity owner, @Nullable LivingEntity candidate) {
        return candidate != null && candidate != owner && CONDITIONS.test(entity, candidate);
    }

}
