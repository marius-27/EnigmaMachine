package com.example.enigmamachine.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.EnigmaMachine;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationFileRobustnessTest {

    private static final String VALID = """
            {
              "formatVersion": 1,
              "rotors": ["I", "II", "III"],
              "reflector": "B",
              "ringSettings": [1, 1, 1],
              "positions": ["A", "A", "A"],
              "plugboard": ["AB"]
            }
            """;
    private static final String NOT_A_CONFIGURATION = "is not a valid Enigma configuration file.";
    private static final String INVALID_SETTINGS = "has invalid settings:";

    @TempDir
    Path folder;

    private final JsonConfigurationRepository repository = new JsonConfigurationRepository();

    @Test
    void manyDifferentConfigurationsSurviveSavingAndLoading() throws Exception {
        Random random = new Random(1918);
        Path file = folder.resolve("machine.json");

        for (int round = 0; round < 500; round++) {
            MachineConfiguration configuration = randomConfiguration(random);

            repository.save(configuration, file);

            assertEquals(configuration, repository.load(file));
        }
    }

    @Test
    void loadedConfigurationEncryptsLikeTheOriginal() throws Exception {
        Random random = new Random(1939);
        Path file = folder.resolve("machine.json");

        for (int round = 0; round < 50; round++) {
            MachineConfiguration configuration = randomConfiguration(random);
            repository.save(configuration, file);

            assertEquals(
                    new EnigmaMachine(configuration).encryptText("THERUSSIANSARECOMING"),
                    new EnigmaMachine(repository.load(file)).encryptText("THERUSSIANSARECOMING"));
        }
    }

    @Test
    void savingAndLoadingAgainGivesTheSameFile() throws Exception {
        Path first = folder.resolve("first.json");
        Path second = folder.resolve("second.json");
        repository.save(randomConfiguration(new Random(1945)), first);

        repository.save(repository.load(first), second);

        assertEquals(Files.readString(first), Files.readString(second));
    }

    @Test
    void aFileSavedByThisVersionKeepsLoading() throws Exception {
        Path file = Files.writeString(folder.resolve("v1.json"), """
                {
                  "formatVersion" : 1,
                  "rotors" : [ "II", "IV", "V" ],
                  "reflector" : "B",
                  "ringSettings" : [ 2, 21, 12 ],
                  "positions" : [ "B", "L", "A" ],
                  "plugboard" : [ "AV", "BS", "CG", "DL", "FU", "HZ", "IN", "KM", "OW", "RX" ]
                }
                """);

        MachineConfiguration loaded = repository.load(file);

        assertEquals(
                "AUFKLXABTEILUNGXVONXKURTINOWAXKURTINOWAXNORDWESTLXSEBEZXSEBE",
                new EnigmaMachine(loaded).encryptText("EDPUDNRGYSZRCXNUYTPOMRMBOFKTBZREZKMLXLVEFGUEYSIOZVEQMIKUBPMM"));
    }

    @Test
    void fieldsMayComeInAnyOrder() throws Exception {
        Path file = Files.writeString(folder.resolve("machine.json"), """
                {
                  "plugboard": ["AB"],
                  "positions": ["A", "A", "A"],
                  "ringSettings": [1, 1, 1],
                  "reflector": "B",
                  "rotors": ["I", "II", "III"],
                  "formatVersion": 1
                }
                """);

        assertEquals(List.of("AB"), repository.load(file).plugboardPairs());
    }

    @Test
    void contentThatIsNotAConfigurationIsExplained() throws IOException {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("empty file", "");
        cases.put("only spaces", "   \n  ");
        cases.put("plain text", "hello");
        cases.put("cut off", VALID.substring(0, VALID.length() / 2));
        cases.put("JSON null", "null");
        cases.put("JSON list", "[1, 2, 3]");
        cases.put("JSON number", "42");
        cases.put("JSON text", "\"I II III\"");
        cases.put("two objects", VALID + VALID);
        cases.put("version as text", VALID.replace("\"formatVersion\": 1", "\"formatVersion\": \"one\""));
        cases.put("rotors as text", VALID.replace("[\"I\", \"II\", \"III\"]", "\"I II III\""));
        cases.put("rotors as object", VALID.replace("[\"I\", \"II\", \"III\"]", "{\"left\": \"I\"}"));
        cases.put("rings as text", VALID.replace("[1, 1, 1]", "[\"a\", \"b\", \"c\"]"));
        cases.put("unknown field", VALID.replace("\"reflector\"", "\"extra\": true, \"reflector\""));

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            assertLoadFailsWith(NOT_A_CONFIGURATION, entry.getValue(), entry.getKey());
        }
    }

    @Test
    void invalidSettingsAreExplained() throws IOException {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("empty object", "{}");
        cases.put("no version", VALID.replace("\"formatVersion\": 1,", ""));
        cases.put("later version", VALID.replace("\"formatVersion\": 1", "\"formatVersion\": 2"));
        cases.put("no rotors", VALID.replace("\"rotors\": [\"I\", \"II\", \"III\"],", ""));
        cases.put("no reflector", VALID.replace("\"reflector\": \"B\",", ""));
        cases.put("no rings", VALID.replace("\"ringSettings\": [1, 1, 1],", ""));
        cases.put("no positions", VALID.replace("\"positions\": [\"A\", \"A\", \"A\"],", ""));
        cases.put("no plugboard", VALID.replace(",\n  \"plugboard\": [\"AB\"]", ""));
        cases.put("two rotors", VALID.replace("[\"I\", \"II\", \"III\"]", "[\"I\", \"II\"]"));
        cases.put("four rotors", VALID.replace("[\"I\", \"II\", \"III\"]", "[\"I\", \"II\", \"III\", \"IV\"]"));
        cases.put("repeated rotor", VALID.replace("[\"I\", \"II\", \"III\"]", "[\"I\", \"I\", \"III\"]"));
        cases.put("unknown rotor", VALID.replace("\"III\"", "\"VIII\""));
        cases.put("lower case rotor", VALID.replace("\"III\"", "\"iii\""));
        cases.put("empty rotor", VALID.replace("\"III\"", "null"));
        cases.put("unknown reflector", VALID.replace("\"reflector\": \"B\"", "\"reflector\": \"A\""));
        cases.put("two rings", VALID.replace("[1, 1, 1]", "[1, 1]"));
        cases.put("ring zero", VALID.replace("[1, 1, 1]", "[0, 1, 1]"));
        cases.put("ring too high", VALID.replace("[1, 1, 1]", "[1, 1, 27]"));
        cases.put("empty ring", VALID.replace("[1, 1, 1]", "[1, null, 1]"));
        cases.put("two positions", VALID.replace("[\"A\", \"A\", \"A\"]", "[\"A\", \"A\"]"));
        cases.put("digit position", VALID.replace("[\"A\", \"A\", \"A\"]", "[\"1\", \"A\", \"A\"]"));
        cases.put("long position", VALID.replace("[\"A\", \"A\", \"A\"]", "[\"AA\", \"A\", \"A\"]"));
        cases.put("empty position", VALID.replace("[\"A\", \"A\", \"A\"]", "[\"\", \"A\", \"A\"]"));
        cases.put("letter with diacritic", VALID.replace("[\"A\", \"A\", \"A\"]", "[\"Ă\", \"A\", \"A\"]"));
        cases.put("one letter pair", VALID.replace("[\"AB\"]", "[\"A\"]"));
        cases.put("self connected pair", VALID.replace("[\"AB\"]", "[\"AA\"]"));
        cases.put("letter in two pairs", VALID.replace("[\"AB\"]", "[\"AB\", \"AC\"]"));
        cases.put("pair with digit", VALID.replace("[\"AB\"]", "[\"A7\"]"));
        cases.put("empty pair", VALID.replace("[\"AB\"]", "[null]"));

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            assertLoadFailsWith(INVALID_SETTINGS, entry.getValue(), entry.getKey());
        }
    }

    @Test
    void bytesThatAreNotTextAreExplained() throws IOException {
        Path file = Files.write(folder.resolve("picture.json"), new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, (byte) 0xFF});

        ConfigurationFileException failure =
                assertThrows(ConfigurationFileException.class, () -> repository.load(file));

        assertTrue(failure.getMessage().startsWith("Could not read picture.json"), failure.getMessage());
    }

    @Test
    void aFolderInsteadOfAFileIsExplained() throws IOException {
        Path inner = Files.createDirectory(folder.resolve("settings.json"));

        ConfigurationFileException loadFailure =
                assertThrows(ConfigurationFileException.class, () -> repository.load(inner));
        ConfigurationFileException saveFailure = assertThrows(
                ConfigurationFileException.class, () -> repository.save(randomConfiguration(new Random(1)), inner));

        assertTrue(loadFailure.getMessage().startsWith("Could not read settings.json"), loadFailure.getMessage());
        assertTrue(saveFailure.getMessage().startsWith("Could not save settings.json"), saveFailure.getMessage());
    }

    @Test
    void failedSaveLeavesTheExistingFileIntact() throws Exception {
        Path inner = Files.createDirectory(folder.resolve("settings.json"));
        Path kept = Files.writeString(inner.resolve("kept.txt"), "keep me");

        assertThrows(ConfigurationFileException.class, () -> repository.save(randomConfiguration(new Random(2)), inner));

        assertEquals("keep me", Files.readString(kept));
        try (var files = Files.list(folder)) {
            assertEquals(List.of(inner), files.toList());
        }
    }

    private void assertLoadFailsWith(String expected, String content, String description) throws IOException {
        Path file = Files.writeString(folder.resolve("case.json"), content);

        ConfigurationFileException failure = assertThrows(
                ConfigurationFileException.class, () -> repository.load(file), description);

        assertTrue(failure.getMessage().contains(expected), description + ": " + failure.getMessage());
        assertTrue(failure.getMessage().contains("case.json"), description + ": " + failure.getMessage());
    }

    private static MachineConfiguration randomConfiguration(Random random) {
        List<RotorDefinition> rotors = new ArrayList<>(Arrays.asList(RotorDefinition.values()));
        Collections.shuffle(rotors, random);
        List<Character> letters = new ArrayList<>();
        for (int index = 0; index < Alphabet.SIZE; index++) {
            letters.add(Alphabet.toLetter(index));
        }
        Collections.shuffle(letters, random);
        List<String> pairs = new ArrayList<>();
        int pairCount = random.nextInt(14);
        for (int pair = 0; pair < pairCount; pair++) {
            pairs.add("" + letters.get(2 * pair) + letters.get(2 * pair + 1));
        }
        return new MachineConfiguration(
                rotors.subList(0, 3),
                Reflector.values()[random.nextInt(Reflector.values().length)],
                List.of(random.nextInt(Alphabet.SIZE), random.nextInt(Alphabet.SIZE), random.nextInt(Alphabet.SIZE)),
                List.of(random.nextInt(Alphabet.SIZE), random.nextInt(Alphabet.SIZE), random.nextInt(Alphabet.SIZE)),
                pairs);
    }
}
