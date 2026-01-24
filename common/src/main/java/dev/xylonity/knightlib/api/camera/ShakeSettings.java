package dev.xylonity.knightlib.api.camera;

import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/**
 * Network-friendly configuration for a camera shake predicate. This is intentionally common safe and
 * the helper packet <b>CameraShakeS2C</b> should be called to apply camera shaking to certain players
 * when working with server-sided logic.
 *
 * @param frequency base shake speed (how fast the noise evolves)
 * @param octaves number of noise layers (higher = more detail)
 * @param persistence amplitude multiplier per octave (lower = smoother)
 * @param lacunarity frequency multiplier per octave (higher = more detail)
 *
 * @see dev.xylonity.knightlib.network.packets.CameraShakeS2C
 */
public record ShakeSettings(
        int durationTicks,
        int fadeInTicks,
        int fadeOutTicks,
        float frequency,
        int octaves,
        float persistence,
        float lacunarity,
        KnightLibEasings easing,
        float amplitudeX,
        float amplitudeY,
        float amplitudeZ,
        long seed
) {

    public ShakeSettings {
        durationTicks = Math.max(1, durationTicks);
        fadeInTicks = Math.max(0, fadeInTicks);
        fadeOutTicks = Math.max(0, fadeOutTicks);

        frequency = Math.max(0.0f, frequency);

        octaves = Mth.clamp(octaves, 1, 8);
        persistence = Mth.clamp(persistence, 0.0f, 0.99f);
        lacunarity = Math.max(1.0f, lacunarity);

        easing = (easing == null) ? KnightLibEasings.SMOOTHSTEP : easing;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(durationTicks);
        buf.writeVarInt(fadeInTicks);
        buf.writeVarInt(fadeOutTicks);

        buf.writeFloat(frequency);

        buf.writeByte(octaves);
        buf.writeFloat(persistence);
        buf.writeFloat(lacunarity);

        buf.writeVarInt(easing.id());

        buf.writeFloat(amplitudeX);
        buf.writeFloat(amplitudeY);
        buf.writeFloat(amplitudeZ);

        buf.writeLong(seed);
    }

    public static ShakeSettings decode(FriendlyByteBuf buf) {
        int duration = Math.max(1, buf.readVarInt());
        int fadeIn = Math.max(0, buf.readVarInt());
        int fadeOut = Math.max(0, buf.readVarInt());

        float frequency = Math.max(0.0f, buf.readFloat());

        int octaves = buf.readByte();
        float persistence = buf.readFloat();
        float lacunarity = buf.readFloat();

        final KnightLibEasings easing = KnightLibEasings.byId(buf.readVarInt());

        float amplitudeX = buf.readFloat();
        float amplitudeY = buf.readFloat();
        float amplitudeZ = buf.readFloat();

        long seed = buf.readLong();

        return new ShakeSettings(duration, fadeIn, fadeOut, frequency, octaves, persistence, lacunarity, easing, amplitudeX, amplitudeY, amplitudeZ, seed);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int durationTicks = 20;
        private int fadeInTicks = 0;
        private int fadeOutTicks = 0;

        private float frequency = 10f;
        private int octaves = 3;
        private float persistence = 0.55f;
        private float lacunarity = 2f;

        private KnightLibEasings easing = KnightLibEasings.LINEAR;

        private float amplitudeX = 0.2f;
        private float amplitudeY = 0.2f;
        private float amplitudeZ = 0.2f;

        private long seed = 1L;

        public Builder durationTicks(int value) {
            this.durationTicks = value;
            return this;
        }

        public Builder fadeInTicks(int value) {
            this.fadeInTicks = value;
            return this;
        }

        public Builder fadeOutTicks(int value) {
            this.fadeOutTicks = value;
            return this;
        }

        public Builder frequency(float value) {
            this.frequency = value;
            return this;
        }

        public Builder octaves(int value) {
            this.octaves = value;
            return this;
        }

        public Builder persistence(float value) {
            this.persistence = value;
            return this;
        }

        public Builder lacunarity(float value) {
            this.lacunarity = value;
            return this;
        }

        public Builder easing(KnightLibEasings easing) {
            this.easing = easing;
            return this;
        }

        public Builder amplitude(float x, float y, float z) {
            this.amplitudeX = x;
            this.amplitudeY = y;
            this.amplitudeZ = z;
            return this;
        }

        public Builder seed(long value) {
            this.seed = value;
            return this;
        }

        public ShakeSettings build() {
            return new ShakeSettings(
                    durationTicks,
                    fadeInTicks,
                    fadeOutTicks,
                    frequency,
                    octaves,
                    persistence,
                    lacunarity,
                    easing,
                    amplitudeX,
                    amplitudeY,
                    amplitudeZ,
                    seed
            );

        }

    }

}