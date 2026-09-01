package dev.xylonity.knightlib.api.client.animation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the playback mode inferred from a vanilla animation field. This is mainly useful for {@link net.minecraft.client.animation.AnimationDefinition}
 * constants discovered through reflection, where the field itself does not otherwise say whether an animation should hold on its last frame.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface KnightLibAnimationMode {

    KnightLibAnimation.LoopMode value();

}