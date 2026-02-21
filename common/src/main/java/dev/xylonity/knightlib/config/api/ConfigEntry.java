package dev.xylonity.knightlib.config.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static field inside an {@link AutoConfig} class as a config entry.
 *
 * <p>Supported field types: {@code boolean}, {@code int}, {@code long}, {@code float},
 * {@code double}, {@code String}, and any {@code enum} type.</p>
 *
 * <pre>{@code
 * @ConfigEntry(
 *     category = "guacamole",
 *     comment  = "The cake is a lie",
 *     min = 0, max = 1,
 *     slider = true
 * )
 * public static double SMALL_ESSENCE_DROP_RATE = 0.125d;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {

    String comment() default "";

    String note() default "";

    String category() default "";

    double min() default Double.NEGATIVE_INFINITY;

    double max() default Double.POSITIVE_INFINITY;

    /**
     * If {@code true} and the field is numeric with finite min/max bounds, the config screen
     * will render a slider instead of a text box.
     */
    boolean slider() default false;

    /**
     * If {@code true}, changing this entry displays a "requires restart" badge in the config
     * screen. This is purely informational.
     */
    boolean requiresRestart() default false;
}