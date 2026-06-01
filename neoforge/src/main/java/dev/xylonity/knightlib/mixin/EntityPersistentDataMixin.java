package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.common.entity.data.internal.KnightLibPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements KnightLibPersistentDataAccessor {

    @Unique
    public CompoundTag knightlib$getPersistentData() {
        return ((Entity) (Object) this).getPersistentData();
    }

}