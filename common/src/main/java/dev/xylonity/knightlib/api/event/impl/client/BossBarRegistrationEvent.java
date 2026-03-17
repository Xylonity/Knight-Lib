package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.bossbar.BossBarBuilder;
import dev.xylonity.knightlib.api.event.KnightLibEvent;

/**
 * Fired during client setup to allow mods to register custom boss bar renderers via {@link BossBarBuilder}
 */
public final class BossBarRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

}