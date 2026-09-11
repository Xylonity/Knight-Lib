package dev.xylonity.knightlib.api.animation;

/**
 * Snapshot of the most recently evaluated animation frame (step). Evaluations refresh this snapshot.
 */
public record KnightLibAnimationPlayback(
        String animation,
        int stepIndex,
        KnightLibAnim.PlaybackMode mode,
        double sequenceTick,
        float animationTick,
        float lengthTicks,
        boolean finished,
        double evaluatedAt
) {

    public float progress() {
        return lengthTicks <= 0f ? 1f : Math.min(1f, Math.max(0f, animationTick / lengthTicks));
    }

    public boolean isLooping() {
        return mode == KnightLibAnim.PlaybackMode.LOOP;
    }

    public boolean isHolding() {
        return mode == KnightLibAnim.PlaybackMode.HOLD_ON_LAST_FRAME && animationTick >= lengthTicks;
    }

}
