package dev.xylonity.knightlib.api.entity.hitbox;

import dev.xylonity.knightlib.api.entity.hitbox.internal.GeoBoneHitboxRig;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Built-in authoritative rig factories
 */
public final class BoneHitboxRigs {

    private BoneHitboxRigs() {
        ;;
    }

    /**
     * Loads geometry and animations from assets packaged in the mod jar
     */
    public static BoneHitboxRig geo(ResourceLocation geometry, ResourceLocation animations) {
        return new GeoBoneHitboxRig(Objects.requireNonNull(geometry, "geometry"), Objects.requireNonNull(animations, "animations"));
    }

    public static BoneHitboxRig geo(ResourceLocation geometry) {
        return new GeoBoneHitboxRig(Objects.requireNonNull(geometry, "geometry"), null);
    }

}
