package dev.xylonity.knightlib.api.entity.data;
 
import dev.xylonity.knightlib.common.entity.data.internal.KnightLibPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
 
/**
 * Return the persistent data of a specific entity.
 *
 * <p>Usage: {@code final CompoundTag tag = PersistentData.get(entity);}</p>
 *
 * <p>The returned CompoundTag is saved and loaded with the entity automatically
 * on both Forge and Fabric.</p>
 */
public final class PersistentData {
 
    private PersistentData() {
        ;;
    }
 
    public static CompoundTag get(Entity entity) {
        return ((KnightLibPersistentDataAccessor) entity).knightlib$getPersistentData();
    }
 
}