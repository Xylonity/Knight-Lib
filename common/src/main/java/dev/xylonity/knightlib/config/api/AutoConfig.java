package dev.xylonity.knightlib.config.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoConfig {

    // Configuration file name
    String file();

    // Overall decoration style (STARSET is the best one)
    DecorationType style() default DecorationType.SIMPLE;

    // Extra comment above the category indicating its own existence (lol)
    boolean categoryBanner() default true;

    // Should be autosaved
    boolean autosave() default true;
}

