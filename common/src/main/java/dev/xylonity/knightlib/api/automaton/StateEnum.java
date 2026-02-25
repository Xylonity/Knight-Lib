package dev.xylonity.knightlib.api.automaton;

/**
 * Contract for state enums used by the {@link Automaton} system.
 *
 * Each constant must provide a stable integer id that uniquely identifies the state.
 */
public interface StateEnum {

    int id();
    String name();

}