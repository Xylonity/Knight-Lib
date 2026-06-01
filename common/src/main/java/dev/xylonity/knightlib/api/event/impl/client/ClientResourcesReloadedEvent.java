package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Fired after client resources (textures, sounds...) are reloaded (F3+T)
 */
public final class ClientResourcesReloadedEvent extends KnightLibEvent {

    private final Minecraft client;
    private final ResourceManager resourceManager;

    public ClientResourcesReloadedEvent(Minecraft client, ResourceManager resourceManager) {
        this.client = client;
        this.resourceManager = resourceManager;
    }

    @Override
    public boolean isSticky() {
        return true;
    }

    public Minecraft getClient() {
        return client;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

}
