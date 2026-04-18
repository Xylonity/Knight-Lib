package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.Targeting;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

/**
 * Sets the attacker as the host's target when the host takes damage from a {@link LivingEntity}.
 *
 * The target is retained for {@code retentionTicks} ticks before being cleared, so a
 * single hit does not produce indefinite aggro. Passing 0 should disable automatic
 * clearing and let whatever else is driving the target slot to take over
 *
 * @param <E> the host entity type
 *
 * @author Xylonity
 */
public class HurtByAttackerTargeting<E extends LivingEntity> implements Targeting<E> {

    private final int retentionTicks;

    private int remainingTicks;

    public HurtByAttackerTargeting() {
        this(200);
    }

    public HurtByAttackerTargeting(int retentionTicks) {
        this.retentionTicks = Math.max(0, retentionTicks);
    }

    @Override
    public void onStart(E entity) {
        remainingTicks = 0;
    }

    @Override
    public void tick(E entity) {
        if (retentionTicks <= 0 || remainingTicks <= 0) {
            return;
        }

        remainingTicks--;
        if (remainingTicks == 0) {
            setTarget(entity, null);
        }

    }

    @Override
    public void onDamaged(E entity, @Nullable DamageSource source, float amount) {
        if (source == null) {
            return;
        }

        final Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker) || attacker == entity) {
            return;
        }

        setTarget(entity, attacker);
        remainingTicks = retentionTicks;
    }

    protected void setTarget(E entity, @Nullable LivingEntity target) {
        if (entity instanceof Mob mob) {
            mob.setTarget(target);
        }

    }

}
