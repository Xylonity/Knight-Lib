package dev.xylonity.knightlib.interfaces.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequiresConfig {

    /**
     * The configuration option that must be enabled for this method or class to function.
     */
    String configOption();

    /**
     * A description of the configuration's purpose.
     */
    String description() default "Requires configuration.";

}