package dev.xylonity.knightlib.common.blockentity;

import dev.xylonity.knightlib.api.animation.*;
import dev.xylonity.knightlib.api.interop.IGreatChaliceInteractable;
import dev.xylonity.knightlib.api.interop.GreatChaliceState;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.Random;

public class GreatChaliceBlockEntity extends BlockEntity implements KnightLibAnimatable {

    private final KnightLibAnimationHandler animations = KnightLibAnimationHandler.of(this);

    private static final KnightLibAnim IDLE = KnightLibAnim.begin().thenLoop("idle");

    private int charges;
    private int prevCharges;

    private GreatChaliceState state;

    private long tickcount;

    public GreatChaliceBlockEntity(BlockPos pos, BlockState state) {
        super(KnightLibBlockEntities.GREAT_CHALICE.get(), pos, state);
        this.state = GreatChaliceState.EMPTY;
        this.charges = 0;
        this.prevCharges = 0;
        this.tickcount = 0;
    }

    public int getCharges() {
        return this.charges;
    }

    public void setCharges(int charges) {
        this.charges = Math.min(charges, IGreatChaliceInteractable.MAX_CHARGES);

        if (charges == 0) setState(GreatChaliceState.EMPTY);

        this.prevCharges = this.charges;
        setChanged();
        sync();
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
        if (!(F instanceof GreatChaliceBlockEntity chalice)) {
            return;
        }

        if (chalice.charges == IGreatChaliceInteractable.MAX_CHARGES && chalice.tickcount % 15 == 0) {
            chalice.spawnSpecialParticles();
        }

        if (chalice.charges == IGreatChaliceInteractable.MAX_CHARGES && chalice.tickcount % 5 == 0) {
            chalice.spawnChaoticParticles();
        }

        if (chalice.tickcount % 5 == 0) {
            chalice.spawnProgressiveParticles(pos);
        }

        chalice.tickcount++;
    }

    private void spawnSpecialParticles() {
        if (level instanceof ServerLevel sv && getState() == GreatChaliceState.NORMAL) {
            for (int i = 0; i < 2; i++) {
                double dx = (new Random().nextDouble() - 0.5) * 0.5;
                double dy = (new Random().nextDouble() - 0.5) * 0.5;
                double dz = (new Random().nextDouble() - 0.5) * 0.5;
                if (i % 3 == 0) {
                    sv.sendParticles(KnightLibParticles.STARSET.get(),
                            this.getBlockPos().getX() + 0.5,
                            this.getBlockPos().getY() + 0.5 * Math.random(),
                            this.getBlockPos().getZ() + 0.5,
                            1, dx, dy, dz, 0.35);
                }
            }
        }

    }

    private void spawnChaoticParticles() {
        if (level instanceof ServerLevel sv && getState() == GreatChaliceState.CHAOTIC) {
            for (int i = 0; i < 2; i++) {
                double dx = (new Random().nextDouble() - 0.5) * 0.5;
                double dy = (new Random().nextDouble() - 0.5) * 0.5;
                double dz = (new Random().nextDouble() - 0.5) * 0.5;
                sv.sendParticles(ParticleTypes.WARPED_SPORE,
                        this.getBlockPos().getX() + 0.5,
                        this.getBlockPos().getY() + 0.5 * Math.random(),
                        this.getBlockPos().getZ() + 0.5,
                        1, dx, dy, dz, 0.3);
            }
            for (int i = 0; i < 2; i++) {
                double dx = (new Random().nextDouble() - 0.5) * 0.5;
                double dy = (new Random().nextDouble() - 0.5) * 0.5;
                double dz = (new Random().nextDouble() - 0.5) * 0.5;
                sv.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        this.getBlockPos().getX() + 0.5,
                        this.getBlockPos().getY() + 0.5 * Math.random(),
                        this.getBlockPos().getZ() + 0.5,
                        1, dx, dy, dz, 0.25);
            }
        }

    }

    private void spawnProgressiveParticles(BlockPos pos) {
        if (level instanceof ServerLevel sv) {
            for (int i = 0; i < this.charges; i++) {
                if (sv.random.nextFloat() < 0.10f) {
                    sv.sendParticles(ParticleTypes.EFFECT,
                            pos.getX() + 0.5 + (sv.random.nextDouble() - 0.5) * 0.9,
                            pos.getY() + ((double) this.getCharges() / 12) + sv.random.nextDouble() * 0.5,
                            pos.getZ() + 0.5 + (sv.random.nextDouble() - 0.5) * 0.9,
                            1, 0.0, 0.1, 0.0, 0.0);
                }
            }
        }

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
    public void registerAnimationControllers(KnightLibAnimationControllerRegistrar controllers) {
        controllers.add("main", this::mainController);
    }

    public KnightLibAnim mainController(final KnightLibAnimationState state) {
        return IDLE;
    }

    @Override
    public KnightLibAnimationHandler getAnimationHandler() {
        return this.animations;
    }

}