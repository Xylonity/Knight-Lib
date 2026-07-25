package dev.xylonity.knightlib.common.entity;

import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractProjectile extends Projectile implements KnightLibAnimatable {

    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(AbstractProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(AbstractProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(AbstractProjectile.class, EntityDataSerializers.FLOAT);

    private final KnightLibAnimationHandler animations = KnightLibAnimationHandler.of(this);

    public AbstractProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (getLifetime() == 0) {
                discardEntity();
            }

            setLifetime(getLifetime() - 1);
        }

    }

    protected void discardEntity() {
        this.discard();
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFETIME, baseLifetime());
        this.entityData.define(MAX_LIFETIME, baseLifetime());
        this.entityData.define(SIZE, 1f);
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public void setSize(float size) {
        this.entityData.set(SIZE, size);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, lifetime);
        if (lifetime > getMaxLifetime()) {
            setMaxLifetime(lifetime);
        }

    }

    public int getMaxLifetime() {
        return this.entityData.get(MAX_LIFETIME);
    }

    public void setMaxLifetime(int lifetime) {
        this.entityData.set(MAX_LIFETIME, lifetime);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Lifetime")) {
            this.setLifetime(pCompound.getInt("Lifetime"));
        }
        if (pCompound.contains("MaxLifetime")) {
            this.setMaxLifetime(pCompound.getInt("MaxLifetime"));
        }
        if (pCompound.contains("EntitySize")) {
            this.setSize(pCompound.getFloat("EntitySize"));
        }

    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Lifetime", this.getLifetime());
        pCompound.putInt("MaxLifetime", this.getMaxLifetime());
        pCompound.putFloat("EntitySize", this.getSize());
    }

    protected abstract int baseLifetime();

    @Override
    public KnightLibAnimationHandler getAnimationHandler() {
        return this.animations;
    }

}
