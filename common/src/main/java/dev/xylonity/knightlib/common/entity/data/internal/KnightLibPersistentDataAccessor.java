package dev.xylonity.knightlib.common.entity.data.internal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public interface KnightLibPersistentDataAccessor {

    CompoundTag knightlib$getPersistentData();

    /**
     * Used for runtime cache
     */
    Map<ResourceLocation, Object> knightlib$getAttachmentCache();
}
