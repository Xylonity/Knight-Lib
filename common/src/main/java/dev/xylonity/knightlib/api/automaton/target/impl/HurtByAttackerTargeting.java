package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.TargetSelector;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import javax.annotation.Nullable;

/**
 * Proposes the attacker as the host's target when the host takes damage from a {@link LivingEntity}
 *
 * The attacker is retained for some retention ticks, so a single hit does not produce
 * indefinite aggro. Passing 0 keeps it until it dies or stops being a valid target
 *
 * @param <E> the host entity type
 *
 * @author Xylonity
 */
public class HurtByAttackerTargeting<E extends Mob> implements TargetSelector<E> {

    private static final TargetingConditions CONDITIONS = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private final int retentionTicks;

    @Nullable
    private LivingEntity attacker;
    private int remainingTicks;

    public HurtByAttackerTargeting() {
        this(200);
    }

    public HurtByAttackerTargeting(int retentionTicks) {
        this.retentionTicks = Math.max(0, retentionTicks);
    }

    @Override
    public void onStart(E entity) {
        attacker = null;
        remainingTicks = 0;
    }

    @Nullable
    @Override
    public LivingEntity select(E entity, @Nullable LivingEntity current) {
        if (attacker != null && (!CONDITIONS.test(entity, attacker) || (retentionTicks > 0 && remainingTicks-- <= 0))) {
            attacker = null;
        }

        return attacker;
    }

    @Override
    public void onDamaged(E entity, @Nullable DamageSource source, float amount) {
        if (source != null && source.getEntity() instanceof LivingEntity living && CONDITIONS.test(entity, living)) {
            attacker = living;
            remainingTicks = retentionTicks;
        }

    }

}
