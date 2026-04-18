package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.Targeting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

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
public class OwnerAssistTargeting<E extends LivingEntity> implements Targeting<E> {

    private final Function<E, LivingEntity> ownerResolver;
    private final int retargetInterval;

    private int tickCounter;

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
    }

    @Override
    public void tick(E entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (tickCounter++ % retargetInterval != 0) {
            return;
        }

        final LivingEntity owner = ownerResolver.apply(entity);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        LivingEntity candidate = null;

        // The owner is already attacking something, so the host joins in
        if (owner instanceof Mob ownerMob) {
            final LivingEntity ownerTarget = ownerMob.getTarget();
            if (ownerTarget != null && ownerTarget.isAlive() && ownerTarget != entity) {
                candidate = ownerTarget;
            }

        }

        // Otherwise the host retaliates against whoever last hit the owner
        if (candidate == null) {
            final LivingEntity hurtBy = owner.getLastHurtByMob();
            if (hurtBy != null && hurtBy.isAlive() && hurtBy != entity && hurtBy != owner) {
                candidate = hurtBy;
            }

        }

        if (candidate != null) {
            setTarget(entity, candidate);
        }

    }

    protected void setTarget(E entity, @Nullable LivingEntity target) {
        if (entity instanceof Mob mob) {
            mob.setTarget(target);
        }

    }

}
