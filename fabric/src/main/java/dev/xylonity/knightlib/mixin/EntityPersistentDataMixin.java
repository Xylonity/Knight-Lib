package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.common.entity.data.internal.KnightLibPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements KnightLibPersistentDataAccessor {

    @Unique
    private CompoundTag knightlib$persistentData = new CompoundTag();

    @Unique
    public CompoundTag knightlib$getPersistentData() {
        return knightlib$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void knightlib$savePersistentData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (!knightlib$persistentData.isEmpty()) {
            tag.put("KnightLibData", knightlib$persistentData.copy());
        }

    }

    @Inject(method = "load", at = @At("RETURN"))
    private void knightlib$loadPersistentData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("KnightLibData", CompoundTag.TAG_COMPOUND)) {
            knightlib$persistentData = tag.getCompound("KnightLibData");
        }

    }

}