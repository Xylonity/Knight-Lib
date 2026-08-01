package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxHolder;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * Lets projectiles collide with animated attackable bone hitboxes, as vanilla only raytraces aabbs
 */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {

    @Inject(
            method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void knightlib$clipBoneHitboxes(Level level, Entity projectile, Vec3 start, Vec3 end, AABB boundingBox, Predicate<Entity> filter, float inflation, CallbackInfoReturnable<EntityHitResult> cir) {
        final EntityHitResult vanillaHit = cir.getReturnValue();
        double bestDistanceSqr = vanillaHit == null
                ? Double.POSITIVE_INFINITY
                : vanillaHit.getLocation().distanceToSqr(start);

        Entity bestEntity = null;
        Vec3 bestLocation = null;
        for (final Entity candidate : level.getEntities(projectile, boundingBox.inflate(2d), filter)) {
            if (!(candidate instanceof BoneHitboxHolder holder)) {
                continue;
            }

            final BoneHitboxManager manager = holder.getBoneHitboxManager();
            if (manager == null || !manager.isActive() || manager.getOwner() != candidate) {
                continue;
            }

            if (level.isClientSide) {
                manager.updateClientPose(1f);
            }

            final Vec3 hit = manager.clipAttackable(start, end);
            if (hit == null) {
                continue;
            }

            final double distanceSqr = hit.distanceToSqr(start);
            if (distanceSqr < bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                bestEntity = candidate;
                bestLocation = hit;
            }

        }

        if (bestEntity != null) {
            cir.setReturnValue(new EntityHitResult(bestEntity, bestLocation));
        }

    }

}