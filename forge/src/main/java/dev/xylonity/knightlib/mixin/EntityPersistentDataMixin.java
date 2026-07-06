package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.common.entity.data.internal.KnightLibPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements KnightLibPersistentDataAccessor {

    @Unique
    private final Map<ResourceLocation, Object> knightlib$attachmentCache = new ConcurrentHashMap<>();

    @Unique
    public CompoundTag knightlib$getPersistentData() {
        return ((Entity) (Object) this).getPersistentData();
    }

    @Unique
    public Map<ResourceLocation, Object> knightlib$getAttachmentCache() {
        return knightlib$attachmentCache;
    }

}
