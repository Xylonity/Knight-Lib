package dev.xylonity.knightlib.client.shader.post.timing;

import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.util.Mth;

/**
 * Computes a normalized intensity curve based on {@link FadeTiming}
 */
public final class FadeEnvelope {

    private final FadeTiming timings;
    private final KnightLibEasings easingIn;
    private final KnightLibEasings easingOut;

    private int ageTicks;

    public FadeEnvelope(FadeTiming timings, KnightLibEasings easingIn, KnightLibEasings easingOut) {
        this.timings = timings;
        this.easingIn = easingIn;
        this.easingOut = easingOut;
    }

    public void tick() {
        int max = timings.fullLengthTicks();
        if (ageTicks < max) {
            ageTicks++;
        }

    }

    public boolean ended() {
        return ageTicks >= timings.fullLengthTicks();
    }

    public int ageTicks() {
        return ageTicks;
    }

    public float value(float partialTick) {
        int fadeInTicks = timings.fadeInTicks();
        int holdTicks = timings.holdTicks();
        int fadeOutTicks = timings.fadeOutTicks();
        int fullLengthTicks = timings.fullLengthTicks();

        if (fullLengthTicks <= 0) {
            return 0f;
        }

        float time = Mth.clamp(ageTicks + partialTick, 0f, (float) fullLengthTicks);

        // Fade in
        if (fadeInTicks > 0 && time < fadeInTicks) {
            float progress = time / (float) fadeInTicks;
            return Mth.clamp(easingIn.apply(progress), 0f, 1f);
        }

        // Hold
        float holdEnd = fadeInTicks + holdTicks;
        if (time >= (float) fadeInTicks && time < holdEnd) {
            return 1f;
        }

        // Fade out
        if (fadeOutTicks > 0) {
            float outStart = fadeInTicks + holdTicks;
            float outTime = time - outStart;
            if (outTime <= 0f) {
                return 1f;
            }

            float progress = Mth.clamp(outTime / (float) fadeOutTicks, 0f, 1f);
            float diff = Mth.clamp(easingOut.apply(progress), 0f, 1f);
            return 1f - diff;
        }

        return (time < fullLengthTicks) ? 1f : 0f;
    }

}
