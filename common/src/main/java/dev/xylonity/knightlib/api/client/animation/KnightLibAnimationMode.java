package dev.xylonity.knightlib.api.client.animation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the inferred playback mode of a code-authored vanilla animation definition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface KnightLibAnimationMode {

    KnightLibAnimation.LoopMode value();

}
