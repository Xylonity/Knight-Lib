package dev.xylonity.knightlib.common.entity;

import dev.xylonity.knightlib.api.automaton.Automaton;
import dev.xylonity.knightlib.api.automaton.AutomatonHandler;
import dev.xylonity.knightlib.api.automaton.StatefulEntity;
import dev.xylonity.knightlib.api.automaton.goal.StateMachineGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class StatefulCompanionEntity<E extends StatefulCompanionEntity<E, S>, S extends Enum<S>> extends TamableAnimal implements StatefulEntity<E, S> {

    private static final EntityDataAccessor<Integer> CURRENT_STATE = SynchedEntityData.defineId(StatefulCompanionEntity.class, EntityDataSerializers.INT);

    private AutomatonHandler<E, S> automatonHandler;

    protected StatefulCompanionEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(CURRENT_STATE, getDefaultState().ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        getAutomatonHandler().tick();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StateMachineGoal<>(selfEntity(), this::getAutomaton));
    }

    @Override
    public AutomatonHandler<E, S> getAutomatonHandler() {
        if (automatonHandler == null) {
            automatonHandler = AutomatonHandler.of(selfEntity(), CURRENT_STATE, this::buildAutomaton, getDefaultState());
        }

        return automatonHandler;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean wasHurt = super.hurt(source, amount);

        if (wasHurt) {
            getAutomatonHandler().onHurt(source, amount);
        }

        return wasHurt;
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        getAutomatonHandler().onDeath();
        super.die(damageSource);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previous = getTarget();
        super.setTarget(target);
        getAutomatonHandler().onTargetChanged(previous, target);
    }

    @Override
    public boolean addEffect(@NotNull MobEffectInstance effectInstance, @Nullable Entity source) {
        boolean applied = super.addEffect(effectInstance, source);

        if (applied) {
            getAutomatonHandler().onEffectAdded(effectInstance);
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
    protected abstract S getDefaultState();

}