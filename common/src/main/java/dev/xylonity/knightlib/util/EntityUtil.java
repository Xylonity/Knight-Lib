package dev.xylonity.knightlib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for handling various entity-related tasks, including
 * entity follow, exploding and leashing.
 *
 * @author Xylonity
 */
public class EntityUtil {

    /**
     * Teleports the player to the specified coordinates.
     *
     * @param entity The player to teleport.
     * @param pos The target position as BlockPos.
     */
    public static void teleportEntity(Entity entity, BlockPos pos) {
        entity.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    /**
     * Causes an entity to follow another entity, similar to how a wolf follows its owner.
     *
     * @param follower The entity that will follow the target.
     * @param target The entity to be followed.
     * @param speed The speed at which the follower will move.
     */
    public static void makeEntityFollow(Entity follower, Entity target, double speed) {
        if (follower instanceof Mob mob && target != null) {
            mob.getNavigation().moveTo(target, speed);
        }
    }

    /**
     * Applies knockback to an entity in a specific direction.
     *
     * @param entity The entity to apply knockback to.
     * @param direction The direction in which the entity will be knocked back.
     * @param force The strength of the knockback.
     */
    public static void applyKnockbackInDirection(Entity entity, Vec3 direction, double force) {
        Vec3 vec3 = direction.normalize().scale(force);
        entity.setDeltaMovement(vec3);
        entity.hurtMarked = true;
    }

    /**
     * Teleports an entity to the highest solid block at its current X and Z coordinates.
     *
     * @param entity The entity to teleport.
     * @param level The server level in which the entity resides.
     */
    public static void teleportToHighestSolidBlock(Entity entity, ServerLevel level) {
        BlockPos currentPos = entity.blockPosition();
        BlockPos highestPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, currentPos);
        entity.teleportTo(highestPos.getX() + 0.5, highestPos.getY(), highestPos.getZ() + 0.5);
    }

    /**
     * This simulates the effect of the leash mechanic in-game.
     *
     * @param entity The entity to be leashed.
     * @param leashHolder The entity or block that will act as the leash holder (e.g., a fence).
     */
    public static void leashEntity(Entity entity, Entity leashHolder) {
        if (entity instanceof Mob mob) {
            mob.setLeashedTo(leashHolder, true);
        }
    }

    /**
     * Slowly pulls an entity toward a target position over a set duration.
     *
     * @param entity The entity being pulled.
     * @param targetPos The target position to pull the entity toward.
     * @param pullStrength The strength of the pull (higher values pull faster).
     */
    public static void pullEntityTowards(Entity entity, Vec3 targetPos, double pullStrength) {
        Vec3 direction = targetPos.subtract(entity.position()).normalize();
        Vec3 pullVector = direction.scale(pullStrength);
        entity.setDeltaMovement(entity.getDeltaMovement().add(pullVector));
        entity.hurtMarked = true;
    }

    /**
     * Launches an entity into the air with a specific velocity.
     *
     * @param entity The entity to launch.
     * @param velocity The velocity at which the entity will be launched.
     */
    public static void launchEntityUpwards(Entity entity, double velocity) {
        entity.setDeltaMovement(new Vec3(0, velocity, 0));
        entity.hurtMarked = true;
    }

    /**
     * Causes an entity to explode at its location, dealing damage to surrounding entities.
     *
     * @param entity The entity that will explode.
     * @param explosionPower The power of the explosion.
     * @param destroysBlocks If true, the explosion will destroy blocks.
     */
    public static void explodeEntity(Entity entity, float explosionPower, boolean destroysBlocks) {
        if (entity.level instanceof ServerLevel level && !entity.level.isClientSide) {
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), explosionPower, destroysBlocks ? Explosion.BlockInteraction.BREAK : Explosion.BlockInteraction.NONE);
            entity.remove(Entity.RemovalReason.KILLED);
        }
    }

    /**
     * Buries an entity underground by teleporting it to a specific Y-level (below the surface).
     *
     * @param entity The entity to bury.
     * @param yLevel The Y-level to teleport the entity to (e.g., underground).
     */
    public static void buryEntity(Entity entity, int yLevel) {
        BlockPos entityPos = entity.blockPosition();
        entity.teleportTo(entityPos.getX(), yLevel, entityPos.getZ());
    }

    /**
     * Summons lightning at an entity's location, striking the entity with lightning damage.
     *
     * @param entity The entity to strike with lightning.
     * @param level The level in which the lightning will strike.
     */
    public static void strikeWithLightning(Entity entity, ServerLevel level) {
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        lightning.moveTo(entity.getX(), entity.getY(), entity.getZ());
        level.addFreshEntity(lightning);
    }

    /**
     * Triggers a random teleportation for an entity within a specified radius.
     *
     * @param entity The entity to teleport.
     * @param radius The maximum radius for the random teleportation.
     */
    public static void randomTeleportWithinRadius(Entity entity, double radius) {
        double randomX = entity.getX() + (Math.random() - 0.5) * radius * 2;
        double randomZ = entity.getZ() + (Math.random() - 0.5) * radius * 2;
        double y = entity.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) randomX, (int) randomZ);
        entity.teleportTo(randomX, y, randomZ);
    }

    /**
     * Checks if an entity is in complete darkness.
     *
     * @param entity The entity to check.
     * @return True if the entity is in complete darkness, false otherwise.
     */
    public static boolean isEntityInCompleteDarkness(Entity entity) {
        return entity.level.getMaxLocalRawBrightness(entity.blockPosition()) == 0;
    }

}