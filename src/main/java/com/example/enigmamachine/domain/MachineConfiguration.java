package com.example.enigmamachine.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Initial machine settings. Rotors, ring settings and positions are ordered left, middle, right.
 */
public record MachineConfiguration(
        List<RotorDefinition> rotors,
        Reflector reflector,
        List<Integer> ringSettings,
        List<Integer> positions,
        List<String> plugboardPairs) {

    private static final int ROTOR_COUNT = 3;

    public MachineConfiguration {
        rotors = requireRotorSlots(rotors, "rotors");
        Objects.requireNonNull(reflector, "Reflector is required.");
        ringSettings = requireRotorSlots(ringSettings, "ring settings");
        positions = requireRotorSlots(positions, "positions");
        plugboardPairs = requireNoNulls(plugboardPairs, "plugboard pairs");

        if (new HashSet<>(rotors).size() != ROTOR_COUNT) {
            throw new IllegalArgumentException("Rotors must be distinct, got: " + rotors + ".");
        }
        ringSettings.forEach(Alphabet::requireIndex);
        positions.forEach(Alphabet::requireIndex);
        new Plugboard(plugboardPairs);
    }

    private static <T> List<T> requireRotorSlots(List<T> values, String name) {
        List<T> copy = requireNoNulls(values, name);
        if (copy.size() != ROTOR_COUNT) {
            throw new IllegalArgumentException("Expected " + ROTOR_COUNT + " " + name + ", got " + copy.size() + ".");
        }
        return copy;
    }

    private static <T> List<T> requireNoNulls(List<T> values, String name) {
        Objects.requireNonNull(values, "Machine configuration requires " + name + ".");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Machine configuration has an empty value in " + name + ".");
        }
        return List.copyOf(values);
    }
}
