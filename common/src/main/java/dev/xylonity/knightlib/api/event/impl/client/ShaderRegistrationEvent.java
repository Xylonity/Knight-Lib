package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Event to register core shaders (ShaderInstance)
 */
public abstract class ShaderRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    public abstract void register(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> onLoaded) throws IOException;

}