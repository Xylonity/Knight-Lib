package dev.xylonity.knightlib.client.entity.layer;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxHolder;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxManager;
import dev.xylonity.knightlib.network.packets.BoneHitboxAttackC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Extends vanilla crosshair picking with animated OBBs that define an
 * {@link dev.xylonity.knightlib.api.entity.hitbox.BoneHitbox#onAttacked} behavior.
 */
public final class BoneHitboxPicker {

    private static @Nullable Selection selection;

    private BoneHitboxPicker() {
        ;;
    }

    public static void pick(Minecraft minecraft, float partialTick) {
        selection = null;
        if (minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        final Entity camera = minecraft.getCameraEntity();
        if (camera == null) {
            return;
        }

        final double reach = minecraft.gameMode.hasFarPickRange() ? 6d : Math.min(3d, minecraft.gameMode.getPickRange());
        final Vec3 start = camera.getEyePosition(partialTick);
        final Vec3 end = start.add(camera.getViewVector(partialTick).scale(reach));

        final HitResult vanillaHit = minecraft.hitResult;
        final double vanillaDistance = vanillaHit == null ? reach : Math.min(reach, vanillaHit.getLocation().distanceTo(start));
        final Entity vanillaEntity = vanillaHit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;

        Entity closestOwner = null;
        BoneHitboxManager.RayHit closestHit = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (final Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity == camera || entity.isSpectator() || !entity.isPickable() || !(entity instanceof BoneHitboxHolder holder)) {
                continue;
            }
            if (entity.distanceToSqr(start) > 32d * 32d) {
                continue;
            }

            final BoneHitboxManager manager = holder.getBoneHitboxManager();
            if (manager == null || manager.getOwner() != entity) {
                continue;
            }
            if (!manager.hasAttackableHitboxes()) {
                continue;
            }

            // Coarse cull to prevent computing the hitbox on non-relevant cases
            final AABB coarse = entity.getBoundingBox().inflate(4d);
            if (!coarse.contains(start) && coarse.clip(start, end).isEmpty()) {
                continue;
            }

            // Refreshes the client right before ray tracing so the picked OBBs always match the current interpolated pose
            manager.updateClientPose(partialTick);

            final BoneHitboxManager.RayHit hit = manager.rayTraceAttackable(start, end);
            if (hit == null || hit.distance() >= closestDistance) {
                continue;
            }

            final double obstructionDistance = vanillaEntity == entity ? reach : vanillaDistance;
            if (hit.distance() > obstructionDistance + 1.0E-6) {
                continue;
            }

            closestOwner = entity;
            closestHit = hit;
            closestDistance = hit.distance();
        }

        if (closestOwner == null || closestHit == null) {
            return;
        }

        minecraft.hitResult = new EntityHitResult(closestOwner, closestHit.location());
        minecraft.crosshairPickEntity = closestOwner;
        selection = new Selection(closestOwner.getId(), closestHit.hitbox().getBoneName());
    }

    /// Returns whether the crosshair currently targets a bone of the given entity, meaning vanilla
    /// entity attacking must be skipped in favor of (attack(entity))
    public static boolean hasSelection(Entity target) {
        final Selection current = selection;
        return current != null && current.entityId() == target.getId();
    }

    /// Sends the selected bone attack to the server
    public static void attack(Entity target) {
        final Selection current = selection;
        if (current == null || current.entityId() != target.getId()) {
            return;
        }

        KnightLib.NET.sendToServer(new BoneHitboxAttackC2S(
                current.entityId(),
                current.boneName()
        ));
    }

    private record Selection(
            int entityId,
            String boneName
    ) {
        ;;
    }

}
