package dev.xylonity.knightlib.api.camera.shake;

import dev.xylonity.knightlib.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;

/**
 * Client-sided camera shake abstraction
 *
 * Notes:
 *  - This service is client-only (as it directly moves the camera), so importing this class in any server-sided login will cause a crash
 *  - Shakes are lazy, as they capture the first tick on the first sampling
 *  - Camera effects are stacked together
 */
public final class CameraShakeManager {

    private static final ArrayDeque<ShakeInstance> STACK = new ArrayDeque<>();
    private static final long UNSTARTED_TICK = Long.MIN_VALUE;

    private CameraShakeManager() {
        ;;
    }

    public static void shake(ShakeSettings settings) {
        shake(settings, false);
    }

    /**
     * For the S2C handler (or client code) without need a player
     * @see dev.xylonity.knightlib.network.packets.CameraShakeS2C
     */
    public static void shake(ShakeSettings settings, boolean replace) {
        if (settings == null) {
            return;
        }
        if (replace) {
            STACK.clear();
        }

        STACK.addLast(new ShakeInstance(settings));
    }

    public static void clearAll() {
        STACK.clear();
    }

    /**
     * Applies a camera shake for this frame, if any.
     */
    public static void applyShakeIfPresent(Player player, Camera camera, float partialTick) {
        if (player == null || camera == null) {
            return;
        }

        Level level = player.level();
        if (STACK.isEmpty()) {
            return;
        }

        STACK.removeIf(s -> s.isExpired(level, partialTick));
        if (STACK.isEmpty()) {
            return;
        }

        double sx = 0;
        double sy = 0;
        double sz = 0;
        for (ShakeInstance shakeInstance : STACK) {
            PositionVector vector = shakeInstance.sample(level, partialTick);
            sx += vector.x;
            sy += vector.y;
            sz += vector.z;
        }

        sx = Mth.clamp((float) sx, -4f, 4f);
        sy = Mth.clamp((float) sy, -4f, 4f);
        sz = Mth.clamp((float) sz, -4f, 4f);

        ((CameraAccessor) camera).moveAccessor(sx, sy, sz);
    }

    /**
     * One active shake instance
     */
    private static final class ShakeInstance {

        private final ShakeSettings settings;

        private long startTick = UNSTARTED_TICK;
        private ResourceKey<Level> dimension;

        private ShakeInstance(ShakeSettings settings) {
            this.settings = settings;
        }

        private void ensureStarted(Level level) {
            if (startTick != UNSTARTED_TICK) {
                return;
            }

            startTick = level.getGameTime();
            dimension = level.dimension();
        }

        /**
         * A shake expires when its elapsed time >= duration, or if the player changes dimension
         */
        boolean isExpired(Level level, float partialTick) {
            if (level == null) {
                return true;
            }
            if (startTick == UNSTARTED_TICK) {
                return false;
            }
            if (level.dimension() != dimension) {
                return true;
            }

            float ticks = (level.getGameTime() - startTick) + partialTick;
            return ticks >= settings.durationTicks();
        }

        /**
         * Samples the shake vector for the current frame. Using fBm (noise) to prevent thread-specific crashes
         */
        PositionVector sample(Level level, float partialTick) {

            ensureStarted(level);

            if (level.dimension() != dimension) {
                return PositionVector.ZERO;
            }

            float ticks = (level.getGameTime() - startTick) + partialTick;
            float envelope = envelope(ticks, settings);
            if (envelope <= 0f) {
                return PositionVector.ZERO;
            }

            float ticksSecond = ticks / 20f;
            float base = ticksSecond * settings.frequency();

            float x = fbm1D(settings.seed() ^ 0xA2F1D3B4C5L, base, settings) * settings.amplitudeX() * envelope;
            float y = fbm1D(settings.seed() ^ 0xC7E91F2A33L, base, settings) * settings.amplitudeY() * envelope;
            float z = fbm1D(settings.seed() ^ 0x9B1C4D5E66L, base, settings) * settings.amplitudeZ() * envelope;

            return new PositionVector(x, y, z);
        }

    }

    private static float envelope(float ticks, ShakeSettings settings) {
        float duration = settings.durationTicks();

        float fadeIn = 1f;
        if (settings.fadeInTicks() > 0) {
            fadeIn = Mth.clamp(ticks / (float) settings.fadeInTicks(), 0f, 1f);
        }

        float fadeOut = 1f;
        if (settings.fadeOutTicks() > 0) {
            float remaining = duration - ticks;
            fadeOut = Mth.clamp(remaining / (float) settings.fadeOutTicks(), 0f, 1f);
        }

        float envelope = fadeIn * fadeOut;
        envelope = settings.easing().apply(Mth.clamp(envelope, 0f, 1f));
        return envelope;
    }

    private static float valueNoise1D(long seed, float value) {
        int i0 = Mth.floor(value);
        float f = value - i0;

        float a = hashSigned(seed + i0);
        float b = hashSigned(seed + i0 + 1);

        float u = f * f * (3.0f - 2.0f * f);
        return Mth.lerp(u, a, b);
    }

    /**
     * fBm algorithm, which computes a sum of multiple octaves of noise at increasing frequency and decreasing amplitude
     *
     * @see ShakeSettings
     */
    private static float fbm1D(long seed, float value, ShakeSettings settings) {
        float amp = 1f;
        float frequency = 1f;
        float sum = 0f;
        float norm = 0f;

        for (int i = 0; i < settings.octaves(); i++) {
            float number = valueNoise1D(seed + (i * 0x9E3779B97F4A7C15L), value * frequency);
            sum += number * amp;
            norm += amp;

            amp *= settings.persistence();
            frequency *= settings.lacunarity();
        }

        return norm > 0f ? (sum / norm) : 0f;
    }

    private static float hashSigned(long value) {
        long z = value;
        z ^= (z >>> 33);
        z *= 0xff51afd7ed558ccdL;
        z ^= (z >>> 33);
        z *= 0xc4ceb9fe1a85ec53L;
        z ^= (z >>> 33);

        int top24 = (int) (z >>> 40);
        float v01 = top24 / (float) (1 << 24);
        return (v01 * 2f) - 1f;
    }

    /**
     * Immutable vector to avoid pulling a Vec3 for just 3 components
     */
    private record PositionVector(
            double x,
            double y,
            double z
    ) {
        static final PositionVector ZERO = new PositionVector(0.0, 0.0, 0.0);
    }

}
