package dev.xylonity.knightlib.api.automaton.target.impl;

import dev.xylonity.knightlib.api.automaton.target.TargetSelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Companion targeting that forwards the owner's combat state onto the host. It defends the owner first, then joins
 * whatever the owner hits, keeping each fight until its target is no longer valid.
 *
 * Ownership is resolved via the supplied {@link Function}, decoupling this class from any specific taming system
 *
 * @param <E> the host entity type
 *
 * @author Xylonity
 */
public class OwnerAssistTargeting<E extends Mob> implements TargetSelector<E> {

    private static final TargetingConditions CONDITIONS = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private final Function<E, LivingEntity> ownerResolver;
    private final int retargetInterval;

    private int tickCounter;
    private int lastHurtByTimestamp;
    private int lastHurtTimestamp;

    @Nullable
    private LivingEntity assisted;
    private boolean defending;

    public OwnerAssistTargeting(Function<E, LivingEntity> ownerResolver) {
        this(ownerResolver, 5);
    }

    public OwnerAssistTargeting(Function<E, LivingEntity> ownerResolver, int retargetInterval) {
        this.ownerResolver = ownerResolver;
        this.retargetInterval = Math.max(1, retargetInterval);
    }

    @Override
    public void onStart(E entity) {
        tickCounter = entity.getRandom().nextInt(retargetInterval);
        assisted = null;
        defending = false;
    }

    @Nullable
    @Override
    public LivingEntity select(E entity, @Nullable LivingEntity current) {
        if (assisted != null && !CONDITIONS.test(entity, assisted)) {
            assisted = null;
            defending = false;
        }

        if (tickCounter++ % retargetInterval == 0) {
            retarget(entity);
        }

        return assisted;
    }

    private void retarget(E entity) {
        final LivingEntity owner = ownerResolver.apply(entity);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        // Timestamps make the host react to new hits only
        if (!defending && owner.getLastHurtByMobTimestamp() != lastHurtByTimestamp && isValidCandidate(entity, owner, owner.getLastHurtByMob())) {
            lastHurtByTimestamp = owner.getLastHurtByMobTimestamp();
            assisted = owner.getLastHurtByMob();
            defending = true;
            return;
        }

        // Otherwise the current fight is kept until it ends
        if (assisted != null) {
            return;
        }

        if (owner.getLastHurtMobTimestamp() != lastHurtTimestamp && isValidCandidate(entity, owner, owner.getLastHurtMob())) {
            lastHurtTimestamp = owner.getLastHurtMobTimestamp();
            assisted = owner.getLastHurtMob();
        }
        else if (owner instanceof Mob ownerMob && isValidCandidate(entity, owner, ownerMob.getTarget())) {
            assisted = ownerMob.getTarget();
        }

    }

    private boolean isValidCandidate(E entity, LivingEntity owner, @Nullable LivingEntity candidate) {
        if (candidate == null || candidate == owner || !CONDITIONS.test(entity, candidate)) {
            return false;
        }

        if (candidate instanceof OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }

        return !(candidate instanceof Player player && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(player));
    }

}
