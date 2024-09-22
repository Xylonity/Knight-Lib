package dev.xylonity.knightlib.interfaces;

public interface IEntityWithAbility {

    /**
     * Applies a special ability or effect to the entity.
     */
    void applyAbility();

    /**
     * Checks if the entity has enough energy or resources to use its ability.
     *
     * @return True if the entity can use its ability, false otherwise.
     */
    boolean canUseAbility();

    /**
     * Triggers an animation or visual effect when the entity uses its ability.
     */
    void triggerAbilityEffect();

}
