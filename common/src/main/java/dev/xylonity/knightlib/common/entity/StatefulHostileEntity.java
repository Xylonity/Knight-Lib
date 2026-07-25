package dev.xylonity.knightlib.common.entity;

import dev.xylonity.knightlib.api.automaton.Automaton;
import dev.xylonity.knightlib.api.automaton.StateEnum;
import dev.xylonity.knightlib.api.automaton.goal.StateMachineGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class StatefulHostileEntity<E extends StatefulHostileEntity<E, S>, S extends Enum<S> & StateEnum> extends Monster {

    private static final EntityDataAccessor<Integer> CURRENT_STATE = SynchedEntityData.defineId(StatefulHostileEntity.class, EntityDataSerializers.INT);

    private Automaton<E, S> automaton;

    protected StatefulHostileEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(CURRENT_STATE, getDefaultState().id());
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && getAutomaton() != null) {
            setCurrentState(getAutomaton().currentStateId());
        }

        // Computes a correct smooth yaw rotation when looking at a target position
        if (level().isClientSide) {
            computeYawRotation();
        }

    }

    public void setCurrentState(int stateId) {
        this.getEntityData().set(CURRENT_STATE, stateId);
    }

    public int getCurrentStateId() {
        return this.getEntityData().get(CURRENT_STATE);
    }

    public S getCurrentState() {
        final int id = getCurrentStateId();
        final S[] states = getStateValues();

        for (S state : states) {
            if (state.id() == id) {
                return state;
            }

        }

        return getDefaultState();
    }

    protected void computeYawRotation() {
        yBodyRot = Mth.approachDegrees(yBodyRot, getYRot(), 6.0f);
        setYHeadRot(Mth.approachDegrees(getYHeadRot(), getYRot(), 12.0f));
    }

    @Override
    protected void registerGoals() {
        this.automaton = buildAutomaton();
        this.goalSelector.addGoal(1, new StateMachineGoal<>(selfEntity(), automaton));
    }

    public Automaton<E, S> getAutomaton() {
        return automaton;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean wasHurt = super.hurt(source, amount);

        if (wasHurt && automaton != null && !this.level().isClientSide) {
            automaton.onDamaged(selfEntity(), source, amount);
        }

        return wasHurt;
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (automaton != null && !this.level().isClientSide) {
            automaton.onDeath(selfEntity());
        }

        super.die(damageSource);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previous = getTarget();
        super.setTarget(target);

        if (automaton != null && !this.level().isClientSide) {
            automaton.onTargetChanged(selfEntity(), previous, target);
        }

    }

    @Override
    public boolean addEffect(@NotNull MobEffectInstance effectInstance, @Nullable Entity source) {
        boolean applied = super.addEffect(effectInstance, source);

        if (applied && automaton != null && !this.level().isClientSide) {
            automaton.onEffectAdded(selfEntity(), effectInstance);
        }

        return applied;
    }

    @SuppressWarnings("unchecked")
    protected final E selfEntity() {
        return (E) this;
    }

    @NotNull
    protected abstract Automaton<E, S> buildAutomaton();

    @NotNull
    protected abstract S[] getStateValues();

    @NotNull
    protected abstract S getDefaultState();

}