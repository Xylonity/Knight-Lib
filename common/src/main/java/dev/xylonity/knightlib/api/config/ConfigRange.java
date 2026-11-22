package dev.xylonity.knightlib.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bounds of a component inside a record used as a {@link ConfigEntry} field. Values outside them are clamped when the
 * file is read, and show in red in the config screen.
 *
 * <pre>{@code
 * public record ArmorValues(
 *         @ConfigRange(min = 0, max = 30) int helmet,
 *         @ConfigRange(min = 0, max = 20) float toughness
 * ) {
 *      ;;
 * }
 *
 * @ConfigEntry(category = "Armor", comment = "Mage armor")
 * public static ArmorValues MAGE_ARMOR = new ArmorValues(3, 2f);
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface ConfigRange {

    double min() default Double.NEGATIVE_INFINITY;

    double max() default Double.POSITIVE_INFINITY;

}
