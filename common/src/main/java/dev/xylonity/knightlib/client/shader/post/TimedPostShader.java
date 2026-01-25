package dev.xylonity.knightlib.client.shader.post;

import dev.xylonity.knightlib.client.shader.post.internal.PostShaderSettings;
import dev.xylonity.knightlib.client.shader.post.timing.FadeEnvelope;
import net.minecraft.client.Minecraft;

/**
 * Small helper for shaders that have a single timed "active instance".
 * It owns a timed envelope and auto-clears when the envelope ends
 */
public abstract class TimedPostShader<PSS extends PostShaderSettings> extends AbstractPostShader<PSS> {

    protected FadeEnvelope envelope;
    protected PSS settings;

    protected abstract FadeEnvelope createEnvelope(PSS settings);

    protected boolean tickWhilePaused() {
        return false;
    }

    @Override
    public void start(PSS settings) {
        this.settings = settings;
        this.envelope = createEnvelope(settings);
    }

    @Override
    public void clientTick() {
        if (envelope == null) {
            return;
        }
        if (!tickWhilePaused() && Minecraft.getInstance().isPaused()) {
            return;
        }

        envelope.tick();
        if (envelope.ended()) {
            clear();
        }

    }

    @Override
    public void clear() {
        envelope = null;
        settings = null;
    }

    protected final float strength(float partialTicks) {
        return envelope == null ? 0f : envelope.value(partialTicks);
    }

    protected final int ageInTicks() {
        return envelope == null ? 0 : envelope.ageTicks();
    }

}
