package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the server when a mob's target is about to change through {@link Mob#setTarget} (goal targeting, mobs
 * targeting through brain memories, like piglins or wardens, don't fire it). Cancelling it keeps the current target
 */
public final class MobChangeTargetEvent extends KnightLibEvent {

    private final Mob mob;
    private LivingEntity newTarget;
    private boolean cancelled = false;

    public MobChangeTargetEvent(Mob mob, @Nullable LivingEntity newTarget) {
        this.mob = mob;
        this.newTarget = newTarget;
    }

    public Mob getMob() {
        return mob;
    }

    @Nullable
    public LivingEntity getCurrentTarget() {
        return mob.getTarget();
    }

    /**
     * Target about to be set, null when the mob is about to lose its target
     */
    @Nullable
    public LivingEntity getNewTarget() {
        return newTarget;
    }

    public void setNewTarget(@Nullable LivingEntity newTarget) {
        this.newTarget = newTarget;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
