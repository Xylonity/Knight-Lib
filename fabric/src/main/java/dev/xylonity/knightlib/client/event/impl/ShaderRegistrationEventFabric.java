package dev.xylonity.knightlib.client.event.impl;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xylonity.knightlib.api.event.impl.client.ShaderRegistrationEvent;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.function.Consumer;

public class ShaderRegistrationEventFabric extends ShaderRegistrationEvent {

    private final CoreShaderRegistrationCallback.RegistrationContext context;

    public ShaderRegistrationEventFabric(CoreShaderRegistrationCallback.RegistrationContext context) {
        this.context = context;
    }

    @Override
    public void register(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> onLoaded) throws IOException {
        context.register(id, format, onLoaded);
    }

}