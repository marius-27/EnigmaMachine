package com.example.enigmamachine.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonConfigurationRepositoryTest {

    private static final MachineConfiguration CONFIGURATION = new MachineConfiguration(
            List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
            Reflector.C,
            List.of(1, 20, 11),
            List.of(1, 11, 0),
            List.of("AV", "BS"));
    private static final MachineConfiguration OTHER_CONFIGURATION = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of());

    @TempDir
    Path folder;

    private final JsonConfigurationRepository repository = new JsonConfigurationRepository();

    @Test
    void loadsWhatWasSaved() throws Exception {
        Path file = folder.resolve("machine.json");

        repository.save(CONFIGURATION, file);

        assertEquals(CONFIGURATION, repository.load(file));
    }

    @Test
    void savingOverAFileReplacesIt() throws Exception {
        Path file = folder.resolve("machine.json");
        repository.save(CONFIGURATION, file);

        repository.save(OTHER_CONFIGURATION, file);

        assertEquals(OTHER_CONFIGURATION, repository.load(file));
    }

    @Test
    void savedFileIsReadableJsonWithItsVersion() throws Exception {
        Path file = folder.resolve("machine.json");

        repository.save(CONFIGURATION, file);

        String json = Files.readString(file);
        assertTrue(json.contains("\"formatVersion\" : 1"), json);
        assertTrue(json.lines().count() > 1, "Expected one setting per line: " + json);
    }

    @Test
    void savingLeavesNoTemporaryFilesBehind() throws Exception {
        repository.save(CONFIGURATION, folder.resolve("machine.json"));

        try (var files = Files.list(folder)) {
            assertEquals(List.of(folder.resolve("machine.json")), files.toList());
        }
    }

    @Test
    void failedSaveExplainsWhyAndLeavesNothingBehind() {
        Path file = folder.resolve("missing-folder").resolve("machine.json");

        ConfigurationFileException failure =
                assertThrows(ConfigurationFileException.class, () -> repository.save(CONFIGURATION, file));

        assertTrue(failure.getMessage().startsWith("Could not save machine.json"), failure.getMessage());
        assertFalse(Files.exists(file));
    }

    @Test
    void missingFileIsExplained() {
        assertLoadFails("The file absent.json does not exist.", folder.resolve("absent.json"));
    }

    @Test
    void textThatIsNotJsonIsExplained() throws IOException {
        Path file = Files.writeString(folder.resolve("notes.json"), "these are my notes");

        assertLoadFails("notes.json is not a valid Enigma configuration file.", file);
    }

    @Test
    void jsonNullIsExplainedInsteadOfCrashing() throws IOException {
        Path file = Files.writeString(folder.resolve("null.json"), "null");

        assertLoadFails("null.json is not a valid Enigma configuration file.", file);
    }

    @Test
    void invalidSettingsAreExplained() throws IOException {
        Path file = Files.writeString(folder.resolve("machine.json"), """
                {
                  "formatVersion": 1,
                  "rotors": ["I", "II", "IX"],
                  "reflector": "B",
                  "ringSettings": [1, 1, 1],
                  "positions": ["A", "A", "A"],
                  "plugboard": []
                }
                """);

        assertLoadFails("machine.json has invalid settings: Unknown rotor: 'IX'.", file);
    }

    private void assertLoadFails(String message, Path file) {
        ConfigurationFileException failure =
                assertThrows(ConfigurationFileException.class, () -> repository.load(file));

        assertEquals(message, failure.getMessage());
    }
}
