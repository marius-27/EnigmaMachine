package com.example.enigmamachine.domain;

import java.util.List;

/**
 * The path one letter took through the machine.
 *
 * @param input the letter that entered the plugboard
 * @param output the letter that left the plugboard and lights the lamp
 * @param positions the rotor positions the signal passed through, after stepping, ordered left, middle, right
 * @param steps every component the signal crossed, in order
 */
public record SignalTrace(char input, char output, List<Integer> positions, List<Step> steps) {

    public SignalTrace {
        positions = List.copyOf(positions);
        steps = List.copyOf(steps);
    }

    /** A component of the signal path, in the order the signal reaches it. */
    public enum Stage {
        PLUGBOARD_IN,
        RIGHT_ROTOR_FORWARD,
        MIDDLE_ROTOR_FORWARD,
        LEFT_ROTOR_FORWARD,
        REFLECTOR,
        LEFT_ROTOR_REVERSE,
        MIDDLE_ROTOR_REVERSE,
        RIGHT_ROTOR_REVERSE,
        PLUGBOARD_OUT
    }

    /** The letter entering and leaving one component. */
    public record Step(Stage stage, char input, char output) {
    }
}
