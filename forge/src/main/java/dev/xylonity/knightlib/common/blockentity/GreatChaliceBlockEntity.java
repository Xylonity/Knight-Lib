package dev.xylonity.knightlib.common.blockentity;

import dev.xylonity.knightlib.common.api.GreatChaliceState;
import dev.xylonity.knightlib.common.api.IGreatChaliceInteractable;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GreatChaliceBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final RawAnimation BOUNCE = RawAnimation.begin().thenPlay("idle");

    private int charges;
    private int prevCharges;

    private GreatChaliceState state;

    public GreatChaliceBlockEntity(BlockPos pos, BlockState state) {
        super(KnightLibBlockEntities.GREAT_CHALICE.get(), pos, state);
        this.state = GreatChaliceState.EMPTY;
        this.charges = 0;
        this.prevCharges = 0;
    }

    public int getCharges() {
        return this.charges;
    }

    public void setCharges(int charges) {
        this.charges = Math.min(charges, IGreatChaliceInteractable.MAX_CHARGES);

        starsetRing();

        this.prevCharges = this.charges;
        setChanged();
        sync();
    }

    private void starsetRing() {
        if (level != null) {
            GreatChaliceStartsetRing ring = KnightLibEntities.GREAT_CHALICE_STARSET_RING.get().create(level);
            if (ring != null) {
                ring.setPos(getBlockPos().getX() + 0.5f, getBlockPos().getY() + 0.015, getBlockPos().getZ() + 0.5f);
                level.addFreshEntity(ring);
            }
        }
    }

    public GreatChaliceState getState() {
        return state;
    }

    public boolean isFull() {
        return this.getCharges() == IGreatChaliceInteractable.MAX_CHARGES;
    }

    public void setState(GreatChaliceState newState) {
        if (this.state == newState) return;
        this.state = newState;
        setChanged();
        sync();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Charges", charges);
        tag.putInt("PrevCharges", prevCharges);
        tag.putString("State", state.getSerializedName());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.charges = tag.getInt("Charges");
        this.prevCharges = tag.getInt("PrevCharges");
        try {
            state = GreatChaliceState.valueOf(tag.getString("State").toUpperCase());
        } catch (Exception e) {
            state = GreatChaliceState.NORMAL;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Charges", charges);
        tag.putInt("PrevCharges", prevCharges);
        tag.putString("State", state.getSerializedName());
        return tag;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T F) {
        if (!(F instanceof GreatChaliceBlockEntity chalice)) return;


    }

    public void sync() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Packet<ClientGamePacketListener> pkt = ClientboundBlockEntityDataPacket.create(this);
        serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false).forEach(p -> p.connection.send(pkt));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 2, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> event) {
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

}