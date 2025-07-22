package dev.xylonity.knightlib.api;

import dev.xylonity.knightlib.mixin.CameraAccessor;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-sided abstraction.
 */
public final class CameraShakeManager {

    private static final Map<UUID, Shake> SHAKES = new ConcurrentHashMap<>();

    private CameraShakeManager() { ;; }


    /**
     * This method should be called with the corresponding params to apply camera shaking on a certain player.
     * @param player the player to apply camera shake
     * @param durationTicks the duration in ticks of the shake effect
     * @param intensityX shake intensity within the x-axis
     * @param intensityY shake intensity within the y-axis
     * @param intensityZ shake intensity within the z-axis
     * @param fadeStartTick fading ticks
     */
    public static void shake(Player player, int durationTicks, float intensityX, float intensityY, float intensityZ, int fadeStartTick) {
        SHAKES.put(player.getUUID(), new Shake(Util.getMillis(), player.level, durationTicks, intensityX, intensityY, intensityZ, fadeStartTick));
    }

    /**
     * Bridge method to shake non-active players, abstractly called from loader-specific implementations and should not be called directly
     * since it has no direct usage.
     */
    public static void applyShakeIfPresent(Player player, Camera camera) {
        Shake shake = SHAKES.get(player.getUUID());
        if (shake == null) return;

        if (shake.isExpired()) {
            SHAKES.remove(player.getUUID());
            return;
        }

        shake.apply(camera);
    }

    public static void clear() {
        SHAKES.values().removeIf(Shake::isExpired);
    }

    /**
     * Private shake interface to tempsave relevant data
     */
    private record Shake(long startMillis, Level lvl, int durationTicks, float ix, float iy, float iz, int fadeStart) {

        void apply(Camera camera) {
            int elapsed = (int) ((Util.getMillis() - startMillis) / 50);
            float fade = 1f;
            if (fadeStart >= 0 && elapsed >= fadeStart) {
                fade = 1f - Math.min(1f, (float) (elapsed - fadeStart) / (durationTicks - fadeStart));
            }

            double x = (lvl.random.nextDouble() - 0.5) * ix * fade;
            double y = (lvl.random.nextDouble() - 0.5) * iy * fade;
            double z = (lvl.random.nextDouble() - 0.5) * iz * fade;

            ((CameraAccessor) camera).moveAccessor(x, y, z);
        }

        boolean isExpired() {
            return (Util.getMillis() - startMillis) / 50 >= durationTicks;
        }

    }

}
