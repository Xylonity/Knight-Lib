package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fired when a living entity dies. Can be cancelled to prevent the death
 */
public class LivingDeathEvent extends KnightLibEvent {

    private final LivingEntity entity;
    private final DamageSource source;
    private boolean cancelled;

    public LivingDeathEvent(LivingEntity entity, DamageSource source) {
        this.entity = entity;
        this.source = source;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public DamageSource getSource() {
        return source;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}