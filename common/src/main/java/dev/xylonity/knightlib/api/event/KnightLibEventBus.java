package dev.xylonity.knightlib.api.event;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class KnightLibEventBus {

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private final Map<Class<? extends KnightLibEvent>, KnightLibEvent> lastEventByClass = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Class<? extends KnightLibEvent>> postedOrder = new CopyOnWriteArrayList<>();

    private final AtomicLong sequentialRegistry = new AtomicLong(0);

    private volatile boolean stickyEnabled = true;

    public void setStickyEnabled(boolean enabled) {
        this.stickyEnabled = enabled;
    }

    /**
     * Registers {@link RegisterEvent} methods in a deterministic dispatched time ordering.
     */
    @SuppressWarnings("unchecked")
    public void register(Class<?> listenerClass) {
        if (listenerClass == null) {
            return;
        }

        Method[] methods = listenerClass.getDeclaredMethods();
        Arrays.sort(methods, Comparator
                .comparing(Method::getName)
                .thenComparing(method -> Arrays.toString(method.getParameterTypes()))
                .thenComparing(method -> Arrays.toString(method.getExceptionTypes()))
        );

        Object instance = null;
        List<Listener> listenerList = new ArrayList<>();

        for (Method method : methods) {
            RegisterEvent annotation = method.getAnnotation(RegisterEvent.class);
            if (annotation == null) {
                continue;
            }

            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1) {
                throw new IllegalArgumentException(
                        "[KnightLib] @RegisterEvent method must have exactly 1 parameter: " + listenerClass.getName() + "#" + method.getName()
                );

            }

            Class<?> eventTypeRaw = parameters[0];
            if (!KnightLibEvent.class.isAssignableFrom(eventTypeRaw)) {
                throw new IllegalArgumentException(
                        "[KnightLib] Listener parameter must extend " + KnightLibEvent.class.getName() + ": " + listenerClass.getName() + "#" + method.getName()
                );

            }

            boolean isStatic = Modifier.isStatic(method.getModifiers());
            if (!isStatic) {
                if (instance == null) {
                    instance = newInstance(listenerClass);
                }

            }

            method.setAccessible(true);

            Class<? extends KnightLibEvent> eventType = (Class<? extends KnightLibEvent>) eventTypeRaw;

            final long order = sequentialRegistry.incrementAndGet();
            Listener listener = new Listener(
                    instance,
                    method,
                    eventType,
                    annotation.priority(),
                    order,
                    annotation.replaySticky()
            );

            listenerList.add(listener);
        }

        if (listenerList.isEmpty()) {
            return;
        }

        // Keeps the listener list sorted deterministically
        synchronized (listeners) {
            listeners.addAll(listenerList);
            listeners.sort(LISTENER_ORDER);
        }

        // Sticky replay (event replay) for newly registered handlers
        if (stickyEnabled) {
            for (Listener listener : listenerList) {
                if (listener.replaySticky) {
                    replayTo(listener);
                }
            }

        }

    }

    /**
     * Side-safe overload registration method
     * @param listenerClassName as the exact package where the target class is located
     */
    public void register(String listenerClassName) {
        try {
            Class<?> clazz = Class.forName(listenerClassName);
            register(clazz);
        }
        catch (ClassNotFoundException exception) {
            throw new IllegalArgumentException("Listener class not found: " + listenerClassName, exception);
        }

    }

    /**
     * Dispatches an event to matching listeners, sticky caching happening only if the event declares itself as sticky
     */
    public void dispatch(KnightLibEvent event) {
        if (event == null) {
            return;
        }

        if (stickyEnabled && event.isSticky()) {
            cache(event);
        }

        for (Listener listener : listeners) {
            if (!listener.eventType.isInstance(event)) {
                continue;
            }

            try {
                listener.method.invoke(listener.instance, event);
            }
            catch (Throwable throwable) {
                throw new RuntimeException(
                        "[KnightLib] Error dispatching event " + event.getClass().getName() + " to " + listener.method.getDeclaringClass().getName() + "#" + listener.method.getName(), throwable
                );

            }

        }

    }

    public void clear() {
        listeners.clear();
        clearStickyCache();
    }

    public void clearStickyCache() {
        lastEventByClass.clear();
        postedOrder.clear();
    }

    public void clearListenersOnly() {
        listeners.clear();
    }

    private void cache(KnightLibEvent event) {
        @SuppressWarnings("unchecked")
        Class<? extends KnightLibEvent> clazz = (Class<? extends KnightLibEvent>) event.getClass();

        if (!lastEventByClass.containsKey(clazz)) {
            // Stable replay order
            postedOrder.add(clazz);
        }

        lastEventByClass.put(clazz, event);
    }

    private void replayTo(Listener listener) {
        if (!stickyEnabled) {
            return;
        }
        if (postedOrder.isEmpty()) {
            return;
        }

        final List<Class<? extends KnightLibEvent>> snapshot = new ArrayList<>(postedOrder);
        for (Class<? extends KnightLibEvent> clazz : snapshot) {
            KnightLibEvent cached = lastEventByClass.get(clazz);
            if (cached == null) {
                continue;
            }

            if (!listener.eventType.isInstance(cached)) {
                continue;
            }

            try {
                listener.method.invoke(listener.instance, cached);
            }
            catch (Throwable throwable) {
                throw new RuntimeException(
                        "[KnightLib] Error replaying cached event " + cached.getClass().getName() + " to " + listener.method.getDeclaringClass().getName() + "#" + listener.method.getName(), throwable
                );

            }

        }

    }

    private Object newInstance(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("[KnightLib] Non-static listener methods require a public/protected no-arg constructor: " + clazz.getName(), e);
        }
        catch (Throwable throwable) {
            throw new RuntimeException("[KnightLib] Failed to instantiate listener class: " + clazz.getName(), throwable);
        }

    }

    private static final Comparator<Listener> LISTENER_ORDER = (listenerA, listenerB) -> {
        int priority = Integer.compare(listenerB.priority, listenerA.priority);
        if (priority != 0) {
            return priority;
        }

        int registrationOrder = Long.compare(listenerA.registrationOrder, listenerB.registrationOrder); // asc
        if (registrationOrder != 0) {
            return registrationOrder;
        }

        int declaringClass = listenerA.method.getDeclaringClass().getName().compareTo(listenerB.method.getDeclaringClass().getName());
        if (declaringClass != 0) {
            return declaringClass;
        }

        int methodName = listenerA.method.getName().compareTo(listenerB.method.getName());
        if (methodName != 0) {
            return methodName;
        }

        return Arrays.toString(listenerA.method.getParameterTypes()).compareTo(Arrays.toString(listenerB.method.getParameterTypes()));
    };

    private record Listener(
            Object instance,
            Method method,
            Class<? extends KnightLibEvent> eventType,
            int priority,
            long registrationOrder,
            boolean replaySticky
    ) {
        ;;
    }

}
