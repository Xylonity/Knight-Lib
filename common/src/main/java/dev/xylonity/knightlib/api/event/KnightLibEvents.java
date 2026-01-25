package dev.xylonity.knightlib.api.event;

/**
 * Global event entrypoints
 */
public final class KnightLibEvents {

    public static final KnightLibEventBus CLIENT = new KnightLibEventBus();
    public static final KnightLibEventBus SERVER = new KnightLibEventBus();

    private KnightLibEvents() {
        ;;
    }

}
