package dev.xylonity.knightlib.client.event.impl;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xylonity.knightlib.api.event.impl.client.ShaderRegistrationEvent;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.util.function.Consumer;

public class ShaderRegistrationEventNeoForge extends ShaderRegistrationEvent {

    private final RegisterShadersEvent forgeEvent;

    public ShaderRegistrationEventNeoForge(RegisterShadersEvent forgeEvent) {
        this.forgeEvent = forgeEvent;
    }

    @Override
    public void register(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> onLoaded) throws IOException {
        forgeEvent.registerShader(
                new ShaderInstance(forgeEvent.getResourceProvider(), id, format),
                onLoaded
        );

    }

}