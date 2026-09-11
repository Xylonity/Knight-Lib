package dev.xylonity.knightlib.api.client.animation.molang;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Values exposed to Molang while an animation is being sampled.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/molang/MolangQueries.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/model/GeoModel.java
 */
public final class MolangContext {

    private static final Logger LOGGER = LoggerFactory.getLogger("KnightLib");

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private float animTime;
    private float totalAnimTime;
    private float controllerSpeed = 1f;
    private Entity entity;
    private Level level;
    private double now;
    private float actorCount;
    private float distanceFromCamera;

    /**
     * Speed-scaled seconds within the current animation loop
     */
    public void setAnimTime(float animTime) {
        this.animTime = animTime;
    }

    /**
     * Speed-scaled seconds since the animation started (doesn't take into account looping)
     */
    public void setTotalAnimTime(float totalAnimTime) {
        this.totalAnimTime = totalAnimTime;
    }

    /**
     * Playback multiplier of the controller currently being sampled
     */
    public void setControllerSpeed(float controllerSpeed) {
        this.controllerSpeed = Float.isFinite(controllerSpeed) ? controllerSpeed : 1f;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
        this.level = entity == null ? null : entity.level();
    }

    /**
     * World sampled by world-level queries when no entity is available
     */
    public void setLevel(Level level) {
        this.level = level;
    }

    public void setActorCount(float actorCount) {
        this.actorCount = Float.isFinite(actorCount) ? actorCount : 0f;
    }

    public void setDistanceFromCamera(float distanceFromCamera) {
        this.distanceFromCamera = Float.isFinite(distanceFromCamera) ? distanceFromCamera : 0f;
    }

    /**
     * Client game time in ticks, including the partial tick
     */
    public void setNow(double now) {
        this.now = now;
    }

    public float query(String name) {
        return switch (name) {
            case "anim_time" -> animTime;
            case "total_anim_time" -> totalAnimTime;
            case "life_time" -> entity != null ? (entity.tickCount + partialTick()) / 20f : (float) (now / 20.0);
            case "controller_speed" -> controllerSpeed;
            case "actor_count" -> actorCount;
            case "time_of_day" -> level == null ? 0f : timeOfDay(level.getDayTime());
            case "moon_phase" -> level == null ? 0f : level.getMoonPhase();
            case "ground_speed" -> entity == null ? 0f : (float) entity.getDeltaMovement().horizontalDistance() * 20f;
            case "vertical_speed" -> entity == null ? 0f : (float) entity.getDeltaMovement().y() * 20f;
            case "yaw_speed" -> entity instanceof LivingEntity living ? Mth.wrapDegrees(living.getYHeadRot() - living.yHeadRotO) : entity == null ? 0f : Mth.wrapDegrees(entity.getYRot() - entity.yRotO);
            case "body_y_rotation" -> entity instanceof LivingEntity living ? living.yBodyRot : entity == null ? 0f : entity.getYRot();
            case "head_y_rotation" -> entity instanceof LivingEntity living ? living.getYHeadRot() : entity == null ? 0f : entity.getYRot();
            case "pitch", "head_x_rotation" -> entity == null ? 0f : entity.getXRot();
            case "limb_swing" -> entity instanceof LivingEntity living ? living.walkAnimation.position(partialTick()) : 0f;
            case "limb_swing_amount" -> entity instanceof LivingEntity living ? living.walkAnimation.speed(partialTick()) : 0f;
            case "is_moving" -> entity != null && entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5 ? 1f : 0f;
            case "is_on_ground" -> entity != null && entity.onGround() ? 1f : 0f;
            case "is_in_water" -> entity != null && entity.isInWater() ? 1f : 0f;
            case "is_in_water_or_rain" -> entity != null && entity.isInWaterOrRain() ? 1f : 0f;
            case "is_on_fire" -> entity != null && entity.isOnFire() ? 1f : 0f;
            case "is_in_lava" -> entity != null && entity.isInLava() ? 1f : 0f;
            case "is_sneaking" -> entity != null && entity.isCrouching() ? 1f : 0f;
            case "is_sprinting" -> entity != null && entity.isSprinting() ? 1f : 0f;
            case "is_alive" -> entity != null && entity.isAlive() ? 1f : 0f;
            case "is_riding" -> entity != null && entity.isPassenger() ? 1f : 0f;
            case "is_swimming" -> entity instanceof LivingEntity living && living.isVisuallySwimming() ? 1f : 0f;
            case "is_using_item" -> entity instanceof LivingEntity living && living.isUsingItem() ? 1f : 0f;
            case "is_fall_flying" -> entity instanceof LivingEntity living && living.isFallFlying() ? 1f : 0f;
            case "is_sleeping" -> entity instanceof LivingEntity living && living.isSleeping() ? 1f : 0f;
            case "is_baby" -> entity instanceof LivingEntity living && living.isBaby() ? 1f : 0f;
            case "health" -> entity instanceof LivingEntity living ? living.getHealth() : 0f;
            case "max_health" -> entity instanceof LivingEntity living ? living.getMaxHealth() : 0f;
            case "hurt_time" -> entity instanceof LivingEntity living ? living.hurtTime : 0f;
            case "distance_from_camera" -> distanceFromCamera;
            default -> {
                if (WARNED.add(name)) {
                    LOGGER.warn("[KnightLib] Unsupported molang query '{}', evaluating as 0", name);
                }

                yield 0f;
            }

        };

    }

    private float partialTick() {
        return (float) (now - Math.floor(now));
    }

    static float timeOfDay(long dayTime) {
        return ((Math.floorMod(dayTime, 24000L) + 6000L) % 24000L) / 24000f;
    }

}