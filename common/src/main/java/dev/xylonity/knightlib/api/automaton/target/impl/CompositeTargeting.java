package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.Targeting;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Stacks several {@link Targeting} instances.
 *
 * Place the highest priority targeting behavior at the end of the list.
 *
 * @param <E> the host entity type
 *
 * @author Xylonity
 */
public class CompositeTargeting<E extends LivingEntity> implements Targeting<E> {

    private final List<Targeting<? super E>> members;

    @SafeVarargs
    public CompositeTargeting(Targeting<? super E>... members) {
        this.members = List.of(members);
    }

    public CompositeTargeting(List<Targeting<? super E>> members) {
        this.members = List.copyOf(members);
    }

    @Override
    public void onStart(E entity) {
        for (final Targeting<? super E> member : members) {
            member.onStart(entity);
        }

    }

    @Override
    public void tick(E entity) {
        for (final Targeting<? super E> member : members) {
            member.tick(entity);
        }

    }

    @Override
    public void onStop(E entity) {
        for (final Targeting<? super E> member : members) {
            member.onStop(entity);
        }

    }

    @Override
    public void onDamaged(E entity, @Nullable DamageSource source, float amount) {
        for (final Targeting<? super E> member : members) {
            member.onDamaged(entity, source, amount);
        }

    }

}
