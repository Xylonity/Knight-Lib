package dev.xylonity.knightlib.interfaces.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface EntityTypeRestricted {

    /**
     * The entity types that are allowed to use this method or class.
     */
    String[] allowedEntityTypes();

}