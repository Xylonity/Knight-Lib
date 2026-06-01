package dev.xylonity.knightlib.api.automaton.behavior;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

/**
 * Per-entity context store (a modified implementation of the blackboard from the behavior tree pattern) for Automaton behaviors.
 *
 * <p>Data is stored in two layers:</p>
 * <ul>
 *   <li><b>Persistent</b> ({@link #set}) survives state transitions.
 *   <li><b>Transient</b> ({@link #setTransient}) are cleared automatically on every state transition.
 * </ul>
 *
 * <p>When reading via {@link #get}, transient data takes priority over persistent.</p>
 */
public class BehaviorContext {

    private final Map<String, Object> data = new Object2ObjectOpenHashMap<>();
    private final Map<String, Object> transientData = new Object2ObjectOpenHashMap<>();

    private int ticksInState;
    private int previousState = -1;
    private boolean interrupted = false;

    /**
     * Retrieves a value by key. Checks transient first, then the persistent one, then the key default
     */
    @SuppressWarnings("unchecked")
    public <T> T get(BehaviorData<T> behaviorData) {
        final String key = behaviorData.key();
        if (transientData.containsKey(key)) {
            return (T) transientData.get(key);
        }
        if (data.containsKey(key)) {
            return (T) data.get(key);
        }

        return behaviorData.getDefault();
    }

    /**
     * Stores a persistent value that survives state transitions
     */
    public <T> void set(BehaviorData<T> key, T value) {
        data.put(key.key(), value);
    }

    /**
     * Stores a transient value that is cleared on the next state transition
     */
    public <T> void setTransient(BehaviorData<T> key, T value) {
        transientData.put(key.key(), value);
    }

    /**
     * Increments a persistent integer key
     */
    public void add(BehaviorData<Integer> key, int amount) {
        set(key, get(key) + amount);
    }

    /**
     * Increments a transient integer key
     */
    public void addTransient(BehaviorData<Integer> key, int amount) {
        setTransient(key, get(key) + amount);
    }

    /**
     * Removes a specific key from both layers
     */
    public void remove(BehaviorData<?> key) {
        data.remove(key.key());
        transientData.remove(key.key());
    }

    /**
     * Clears transient data only. Called automatically by the automaton on state transitions
     */
    public void clearTransient() {
        transientData.clear();
    }

    /**
     * Clears all data (both persistent and transient)
     */
    public void clear() {
        data.clear();
        transientData.clear();
    }

    public void setTicksInState(int ticks) {
        this.ticksInState = ticks;
    }

    public void setPreviousState(int state) {
        this.previousState = state;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public int ticksInState() {
        return ticksInState;
    }

    public int previousState() {
        return previousState;
    }

    public boolean wasInterrupted() {
        return interrupted;
    }

}