package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.client.shader.post.PostShader;

/**
 * Fired once during initialization to register custom post-processing shaders
 */
public final class RegisterPostShadersEvent extends KnightLibEvent {

    private final Registrar registrar;

    public RegisterPostShadersEvent(Registrar registrar) {
        this.registrar = registrar;
    }

    public void registerShader(PostShader<?> shader) {
        if (shader == null) {
            return;
        }

        registrar.register(shader);
    }

    @Override
    public boolean isSticky() {
        return true;
    }

    @FunctionalInterface
    public interface Registrar {
        void register(PostShader<?> shader);
    }

}