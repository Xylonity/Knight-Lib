package dev.xylonity.knightlib.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static field inside an {@link AutoConfig} class as a config entry.
 *
 * The supported field types are: {@code boolean}, {@code int}, {@code long}, {@code float},
 * {@code double}, {@code String}, any {@code enum} type, {@code List}s of {@code String},
 * {@code Integer}, {@code Long}, {@code Float}, {@code Double} or an {@code enum}, and records whose components
 * are {@code int}, {@code long}, {@code float} or {@code double}, each bounded with {@link ConfigRange}.
 *
 * <pre>{@code
 * @ConfigEntry(
 *     category = "guacamole",
 *     comment  = "The cake is a lie",
 *     min = 0, max = 1,
 *     slider = true
 * )
 * public static double SMALL_ESSENCE_DROP_RATE = 0.125d;
 *
 * @ConfigEntry(
 *     category = "years from now",
 *     comment  = "Mobs that never drop essence",
 *     registry = "minecraft:entity_type"
 * )
 * public static List<String> ESSENCE_BLACKLIST = new ArrayList<>();
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {

    String comment() default "";

    String note() default "";

    String category() default "";

    /**
     * Lower bound for numeric fields. On numeric lists it applies to every element
     */
    double min() default Double.NEGATIVE_INFINITY;

    /**
     * Upper bound for numeric fields. On numeric lists it applies to every element
     */
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

    /**
     * Fixed set of values the player picks from instead of typing them. On a single value field
     * the screen shows a dropdown to pick one, on a list field it lets the player pick any number of them.
     * Values are written as they would be typed in the TOML file
     */
    String[] options() default {};

    /**
     * Like {@link #options()}, but the values are the ids of a game registry, for example
     * "minecraft:entity_type", "minecraft:item" or "minecraft:worldgen/biome", working on string and list
     * fields. Registries that only exist inside a world (biomes, dimensions...) can only be picked from
     * while one is loaded, otherwise the screen falls back to typing the ids. This is ignored if {@link #options()} is set
     */
    String registry() default "";

    /**
     * On a {@link #registry()} entry, also offers that registry's tags, saved as {@code #namespace:path}. Tags only
     * exist inside a world, outside one they are typed in the search field starting with # (he code reading the
     * value has to resolve them itself)
     */
    boolean tags() default false;

    /**
     * Minimum number of elements for {@code List} fields, 0 means no minimum. A list with fewer is ignored when the
     * file is read, and the config screen doesn't let it go below
     */
    int minSize() default 0;

    /**
     * Maximum number of elements for {@code List} fields, 0 means no limit
     */
    int maxSize() default 0;
}