package com.example.enigmamachine.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigurationFileTest {

    /** The format exactly as it is stored. Changing it means files already saved stop loading. */
    private static final String STORED_JSON = """
            {
              "formatVersion": 1,
              "rotors": ["II", "IV", "V"],
              "reflector": "B",
              "ringSettings": [2, 21, 12],
              "positions": ["B", "L", "A"],
              "plugboard": ["AV", "BS", "CG"]
            }
            """;

    private static final MachineConfiguration CONFIGURATION = new MachineConfiguration(
            List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
            Reflector.B,
            List.of(1, 20, 11),
            List.of(1, 11, 0),
            List.of("AV", "BS", "CG"));

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void writesTheStoredFormat() throws JsonProcessingException {
        assertEquals(
                mapper.readTree(STORED_JSON),
                mapper.readTree(mapper.writeValueAsString(ConfigurationFile.from(CONFIGURATION))));
    }

    @Test
    void readsTheStoredFormat() throws JsonProcessingException {
        ConfigurationFile file = mapper.readValue(STORED_JSON, ConfigurationFile.class);

        assertEquals(CONFIGURATION, file.toConfiguration());
    }

    @Test
    void marksFilesWithTheCurrentVersion() {
        assertEquals(1, ConfigurationFile.CURRENT_VERSION);
        assertEquals(1, ConfigurationFile.from(CONFIGURATION).formatVersion());
    }

    @Test
    void conversionKeepsEverySetting() {
        assertEquals(CONFIGURATION, ConfigurationFile.from(CONFIGURATION).toConfiguration());
    }

    @Test
    void lowestAndHighestRingsAndPositionsAreKept() {
        for (int index : List.of(0, 25)) {
            MachineConfiguration edge = new MachineConfiguration(
                    CONFIGURATION.rotors(), Reflector.C, List.of(index, index, index), List.of(index, index, index), List.of());

            assertEquals(edge, ConfigurationFile.from(edge).toConfiguration());
        }
    }

    @Test
    void labelsSettingsLikeTheMachine() {
        ConfigurationFile file = ConfigurationFile.from(CONFIGURATION);

        assertEquals(List.of("II", "IV", "V"), file.rotors());
        assertEquals(List.of(2, 21, 12), file.ringSettings());
        assertEquals(List.of("B", "L", "A"), file.positions());
    }

    @Test
    void rejectsOtherFormatVersions() {
        for (int version : List.of(0, 2, -1)) {
            assertRejected("Unsupported format version " + version + ". Expected 1.", new ConfigurationFile(
                    version, List.of("I", "II", "III"), "B", List.of(1, 1, 1), List.of("A", "A", "A"), List.of()));
        }
    }

    @Test
    void rejectsUnknownNames() {
        assertRejected("Unknown rotor: 'IX'.", new ConfigurationFile(
                1, List.of("I", "II", "IX"), "B", List.of(1, 1, 1), List.of("A", "A", "A"), List.of()));
        assertRejected("Unknown reflector: 'A'.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "A", List.of(1, 1, 1), List.of("A", "A", "A"), List.of()));
    }

    @Test
    void rejectsValuesOutsideTheirRange() {
        assertRejected("Invalid ring setting: 0. Expected 1-26.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", List.of(0, 1, 1), List.of("A", "A", "A"), List.of()));
        assertRejected("Invalid ring setting: 27. Expected 1-26.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", List.of(27, 1, 1), List.of("A", "A", "A"), List.of()));
        assertRejected("Invalid position: '7'. Expected a letter A-Z.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", List.of(1, 1, 1), List.of("7", "A", "A"), List.of()));
        assertRejected("Invalid position: 'AB'. Expected a letter A-Z.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", List.of(1, 1, 1), List.of("AB", "A", "A"), List.of()));
    }

    @Test
    void rejectsMissingSettings() {
        assertRejected("The file has no rotors.", new ConfigurationFile(
                1, null, "B", List.of(1, 1, 1), List.of("A", "A", "A"), List.of()));
        assertRejected("The file has no reflector.", new ConfigurationFile(
                1, List.of("I", "II", "III"), null, List.of(1, 1, 1), List.of("A", "A", "A"), List.of()));
        assertRejected("The file has no ring settings.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", null, List.of("A", "A", "A"), List.of()));
        assertRejected("The file has no positions.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", List.of(1, 1, 1), null, List.of()));
        assertRejected("The file has no plugboard.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", List.of(1, 1, 1), List.of("A", "A", "A"), null));
    }

    @Test
    void rejectsEmptyValuesInsideLists() {
        assertRejected("The file has an empty value in positions.", new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", List.of(1, 1, 1), Arrays.asList("A", null, "A"), List.of()));
    }

    @Test
    void rulesOfTheMachineStillApply() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationFile(
                1, List.of("I", "I", "III"), "B", List.of(1, 1, 1), List.of("A", "A", "A"), List.of()).toConfiguration());
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationFile(
                1, List.of("I", "II"), "B", List.of(1, 1), List.of("A", "A"), List.of()).toConfiguration());
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationFile(
                1, List.of("I", "II", "III"), "B", List.of(1, 1, 1), List.of("A", "A", "A"), List.of("AB", "AC")).toConfiguration());
    }

    private static void assertRejected(String message, ConfigurationFile file) {
        assertEquals(message, assertThrows(IllegalArgumentException.class, file::toConfiguration).getMessage());
    }
}
