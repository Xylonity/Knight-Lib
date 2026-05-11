package dev.xylonity.knightlib.client.shader.post.interop;

/**
 * Marker interface for per-shader configuration data.
 */
public interface PostShaderSettings {

    /**
     * How long (in client ticks) this effect should stay alive.
     * Return {@code -1} for "indefinite/managed externally".
     */
    default int durationTicks() {
        return -1;
    }

    default boolean isFinished(int currentTick, int startTick) {
        final int durationTicks = durationTicks();
        return durationTicks >= 0 && (currentTick - startTick) >= durationTicks;
    }

}
