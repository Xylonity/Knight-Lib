package dev.xylonity.knightlib.client.shader.post.timing;

/**
 * Simple timing container for a (fade-in -> hold -> fade-out) envelope (in ticks)
 */
public record FadeTiming(
        int fadeInTicks,
        int holdTicks,
        int fadeOutTicks
) {

    public FadeTiming {
        if (fadeInTicks < 0 || holdTicks < 0 || fadeOutTicks < 0) {
            throw new IllegalArgumentException("Fade timings cannot be negative");
        }

    }

    public int fullLengthTicks() {
        return fadeInTicks + holdTicks + fadeOutTicks;
    }

    public static FadeTiming of(int fadeInTicks, int holdTicks, int fadeOutTicks) {
        return new FadeTiming(fadeInTicks, holdTicks, fadeOutTicks);
    }

}