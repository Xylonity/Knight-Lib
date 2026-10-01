package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.TargetSelector;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Stacks several {@link TargetSelector} instances.
 *
 * Place the highest priority targeting behavior at the start of the list.
 *
 * @param <E> the host entity type
 *
 * @author Xylonity
 */
public class CompositeTargeting<E extends Mob> implements TargetSelector<E> {

    private final List<TargetSelector<? super E>> members;

    @SafeVarargs
    public CompositeTargeting(TargetSelector<? super E>... members) {
        this.members = List.of(members);
    }

    public CompositeTargeting(List<TargetSelector<? super E>> members) {
        this.members = List.copyOf(members);
    }

    @Override
    public void onStart(E entity) {
        for (final TargetSelector<? super E> member : members) {
            member.onStart(entity);
        }

    }

    @Nullable
    @Override
    public LivingEntity select(E entity, @Nullable LivingEntity current) {
        LivingEntity selected = null;

        for (final TargetSelector<? super E> member : members) {
            final LivingEntity candidate = member.select(entity, current);
            if (selected == null) {
                selected = candidate;
            }

        }

        return selected;
    }

    @Override
    public void onStop(E entity) {
        for (final TargetSelector<? super E> member : members) {
            member.onStop(entity);
        }

    }

    @Override
    public void onDamaged(E entity, @Nullable DamageSource source, float amount) {
        for (final TargetSelector<? super E> member : members) {
            member.onDamaged(entity, source, amount);
        }

    }

}
