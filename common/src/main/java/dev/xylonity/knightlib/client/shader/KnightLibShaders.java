package dev.xylonity.knightlib.client.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.event.impl.client.ShaderRegistrationEvent;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.Objects;

public final class KnightLibShaders {

    private static final ResourceLocation ENTITY_UNSHADED_EMISSIVE = KnightLib.of("rendertype_entity_unshaded_emissive");

    private static ShaderInstance entityEmissive;

    public static void register(ShaderRegistrationEvent event) throws IOException {
        event.register(ENTITY_UNSHADED_EMISSIVE, DefaultVertexFormat.NEW_ENTITY, shader -> entityEmissive = shader);
    }

    public static ShaderInstance getEntityEmissive() {
        return Objects.requireNonNull(entityEmissive, "KnightLib emissive shader has not been loaded properly");
    }

}