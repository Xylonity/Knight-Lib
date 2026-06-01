package dev.xylonity.knightlib.api.event;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RegisterEvent {

    /**
     * Higher priority runs earlier
     */
    int priority() default 0;

    /**
     * If true, this handler will receive replayed sticky events upon registration (only for events that are sticky)
     */
    boolean replaySticky() default true;
}
