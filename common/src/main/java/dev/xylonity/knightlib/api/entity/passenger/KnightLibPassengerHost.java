package dev.xylonity.knightlib.api.entity.passenger;

import net.minecraft.world.entity.Entity;

/**
 * Rideable entity that draws its passengers itself, usually on a seat bone through a
 * {@code dev.xylonity.knightlib.client.animation.layer.impl.KnightLibPassengerLayer}. KnightLib skips those passengers
 * in the entity pass, so they are not drawn. A skipped passenger is only visible through its vehicle's renderer
 */
public interface KnightLibPassengerHost {

    /**
     * Client only (and checked every frame). Whether this vehicle renders {@code passenger} itself
     */
    default boolean rendersPassenger(Entity passenger) {
        return true;
    }

}
