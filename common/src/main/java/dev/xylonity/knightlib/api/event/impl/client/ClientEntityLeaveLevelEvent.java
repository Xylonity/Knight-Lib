package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Fired when an entity is removed from the client level
 */
public final class ClientEntityLeaveLevelEvent extends KnightLibEvent {

    private final Minecraft client;
    private final Level level;
    private final Entity entity;

    public ClientEntityLeaveLevelEvent(Minecraft client, Level level, Entity entity) {
        this.client = client;
        this.level = level;
        this.entity = entity;
    }

    public Minecraft getClient() {
        return client;
    }

    public Level getLevel() {
        return level;
    }

    public Entity getEntity() {
        return entity;
    }

}
