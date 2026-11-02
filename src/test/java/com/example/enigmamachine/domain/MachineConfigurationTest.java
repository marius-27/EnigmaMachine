package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MachineConfigurationTest {

    private static final List<RotorDefinition> ROTORS =
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III);
    private static final List<Integer> ZEROS = List.of(0, 0, 0);

    @Test
    void keepsValidSettings() {
        MachineConfiguration configuration = new MachineConfiguration(
                ROTORS, Reflector.C, List.of(1, 2, 3), List.of(4, 5, 6), List.of("AM"));

        assertEquals(ROTORS, configuration.rotors());
        assertEquals(Reflector.C, configuration.reflector());
        assertEquals(List.of(1, 2, 3), configuration.ringSettings());
        assertEquals(List.of(4, 5, 6), configuration.positions());
        assertEquals(List.of("AM"), configuration.plugboardPairs());
    }

    @Test
    void isNotAffectedByChangesToSourceLists() {
        List<String> pairs = new ArrayList<>(List.of("AM"));
        MachineConfiguration configuration = new MachineConfiguration(ROTORS, Reflector.B, ZEROS, ZEROS, pairs);

        pairs.add("BT");

        assertEquals(List.of("AM"), configuration.plugboardPairs());
    }

    @Test
    void exposesUnmodifiableLists() {
        MachineConfiguration configuration = new MachineConfiguration(ROTORS, Reflector.B, ZEROS, ZEROS, List.of());

        assertThrows(UnsupportedOperationException.class, () -> configuration.plugboardPairs().add("AM"));
    }

    @Test
    void rejectsRepeatedRotors() {
        List<RotorDefinition> repeated = List.of(RotorDefinition.I, RotorDefinition.I, RotorDefinition.III);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new MachineConfiguration(repeated, Reflector.B, ZEROS, ZEROS, List.of()));

        assertTrue(error.getMessage().contains("distinct"));
    }

    @Test
    void rejectsWrongNumberOfRotorValues() {
        List<RotorDefinition> tooFew = List.of(RotorDefinition.I, RotorDefinition.II);
        List<Integer> tooMany = List.of(0, 0, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> new MachineConfiguration(tooFew, Reflector.B, ZEROS, ZEROS, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new MachineConfiguration(ROTORS, Reflector.B, tooMany, ZEROS, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new MachineConfiguration(ROTORS, Reflector.B, ZEROS, tooMany, List.of()));
    }

    @Test
    void rejectsValuesOutsideAlphabet() {
        List<Integer> outside = List.of(0, 0, 26);

        assertThrows(IllegalArgumentException.class,
                () -> new MachineConfiguration(ROTORS, Reflector.B, outside, ZEROS, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new MachineConfiguration(ROTORS, Reflector.B, ZEROS, outside, List.of()));
    }

    @Test
    void rejectsInvalidPlugboardPairs() {
        assertThrows(IllegalArgumentException.class,
                () -> new MachineConfiguration(ROTORS, Reflector.B, ZEROS, ZEROS, List.of("AM", "AB")));
    }

    @Test
    void rejectsMissingSettings() {
        assertThrows(NullPointerException.class,
                () -> new MachineConfiguration(null, Reflector.B, ZEROS, ZEROS, List.of()));
        assertThrows(NullPointerException.class,
                () -> new MachineConfiguration(ROTORS, null, ZEROS, ZEROS, List.of()));
        assertThrows(NullPointerException.class,
                () -> new MachineConfiguration(ROTORS, Reflector.B, null, ZEROS, List.of()));
        assertThrows(NullPointerException.class,
                () -> new MachineConfiguration(ROTORS, Reflector.B, ZEROS, null, List.of()));
        assertThrows(NullPointerException.class,
                () -> new MachineConfiguration(ROTORS, Reflector.B, ZEROS, ZEROS, null));
    }

    @Test
    void rejectsEmptyValuesInsideLists() {
        List<RotorDefinition> withEmptySlot = Arrays.asList(RotorDefinition.I, null, RotorDefinition.III);

        assertThrows(IllegalArgumentException.class,
                () -> new MachineConfiguration(withEmptySlot, Reflector.B, ZEROS, ZEROS, List.of()));
    }
}
