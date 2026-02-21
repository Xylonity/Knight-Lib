package dev.xylonity.knightlib.config.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a KnightLib configuration.
 * All static fields annotated with {@link ConfigEntry} will be serialized to a TOML file.
 *
 * <pre>{@code
 * @AutoConfig(
 *      file = "yeah",
 *      title = "The cake is a lie",
 *      accentColor = 0xFFFFFFFF)
 * public final class ConfigClass {
 *     ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoConfig {

    /**
     * Configuration file name (without extension). The file will be saved as {@code <file>.toml}
     * inside the config directory.
     */
    String file();

    /**
     * Title shown in the config screen header.
     * If empty, defaults to a prettified version of {@link #file()}.
     */
    String title() default "";

    /**
     * Short description shown below the title in the config screen.
     */
    String description() default "";

    /**
     * Decoration style for TOML file comments.
     */
    DecorationType style() default DecorationType.SIMPLE;

    /**
     * Whether to add a decorative banner comment above each category section in the TOML file.
     */
    boolean categoryBanner() default false;

    /**
     * Order hint when this config appears alongside other configs from the same mod
     * in the bridge screen. Lower values appear first.
     */
    int order() default 0;

    /**
     * Accent color for the config screen UI elements (left bar, hover glow, scrollbar, toggle, slider, etc.),
     * specified as an ARGB int.
     */
    int accentColor() default 0xFF4A9EFF;
}