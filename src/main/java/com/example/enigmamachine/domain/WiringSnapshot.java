package com.example.enigmamachine.domain;

import java.util.List;
import java.util.Objects;

/**
 * Where every contact of every component leads at one moment, worked out by the components themselves, so a
 * picture of the machine never needs its own copy of the encryption formulas. A contact is numbered by the letter
 * of the fixed machine frame it lines up with: A is 0 and Z is 25.
 *
 * @param rotors the rotors ordered left, middle, right
 * @param reflector the reflector in use
 * @param reflectorWiring for each contact, the contact the reflector sends the signal back to
 * @param plugboardWiring for each letter, the letter the plugboard swaps it with, or itself when unplugged
 */
public record WiringSnapshot(
        List<RotorWiring> rotors, Reflector reflector, List<Integer> reflectorWiring, List<Integer> plugboardWiring) {

    public WiringSnapshot {
        rotors = List.copyOf(rotors);
        Objects.requireNonNull(reflector, "Reflector is required.");
        reflectorWiring = requireMapping(reflectorWiring);
        plugboardWiring = requireMapping(plugboardWiring);
    }

    /**
     * One rotor at its current position.
     *
     * @param definition the rotor in this slot
     * @param position the letter in its window, as an index
     * @param ringSetting the ring setting, as an index (0 is ring 01)
     * @param forward for each contact on the right, the contact on the left the signal leaves from; the signal
     *                coming back from the reflector follows the same wires the other way
     */
    public record RotorWiring(RotorDefinition definition, int position, int ringSetting, List<Integer> forward) {

        public RotorWiring {
            Objects.requireNonNull(definition, "Rotor definition is required.");
            Alphabet.requireIndex(position);
            Alphabet.requireIndex(ringSetting);
            forward = requireMapping(forward);
        }
    }

    private static List<Integer> requireMapping(List<Integer> mapping) {
        if (mapping.size() != Alphabet.SIZE) {
            throw new IllegalArgumentException("Expected " + Alphabet.SIZE + " contacts, got " + mapping.size() + ".");
        }
        mapping.forEach(Alphabet::requireIndex);
        return List.copyOf(mapping);
    }
}
