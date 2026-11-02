package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class DecryptionTest {

    private static final String MESSAGE = "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG".repeat(30);

    @Test
    void decryptsAfterResetToInitialSettings() {
        for (MachineConfiguration configuration : configurations()) {
            EnigmaMachine machine = new EnigmaMachine(configuration);
            String ciphertext = process(machine, MESSAGE);

            machine.reset();

            assertEquals(MESSAGE, process(machine, ciphertext), configuration.toString());
        }
    }

    @Test
    void decryptsOnSeparateMachineWithSameConfiguration() {
        for (MachineConfiguration configuration : configurations()) {
            String ciphertext = process(new EnigmaMachine(configuration), MESSAGE);

            assertEquals(MESSAGE, process(new EnigmaMachine(configuration), ciphertext), configuration.toString());
        }
    }

    @Test
    void doesNotDecryptFromAlreadyAdvancedPositions() {
        EnigmaMachine machine = new EnigmaMachine(configurations().getFirst());
        String ciphertext = process(machine, MESSAGE);

        assertNotEquals(MESSAGE, process(machine, ciphertext));
    }

    @Test
    void producesSameOutputForSameConfiguration() {
        for (MachineConfiguration configuration : configurations()) {
            assertEquals(
                    process(new EnigmaMachine(configuration), MESSAGE),
                    process(new EnigmaMachine(configuration), MESSAGE),
                    configuration.toString());
        }
    }

    private static List<MachineConfiguration> configurations() {
        return List.of(
                new MachineConfiguration(
                        List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                        Reflector.B,
                        List.of(0, 0, 0),
                        List.of(0, 0, 0),
                        List.of()),
                new MachineConfiguration(
                        List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
                        Reflector.C,
                        List.of(1, 20, 11),
                        List.of(1, 11, 0),
                        List.of("AV", "BS", "CG", "DL", "FU", "HZ", "IN", "KM", "OW", "RX")),
                new MachineConfiguration(
                        List.of(RotorDefinition.V, RotorDefinition.III, RotorDefinition.I),
                        Reflector.B,
                        List.of(25, 13, 7),
                        List.of(16, 4, 21),
                        List.of("QZ")));
    }

    private static String process(EnigmaMachine machine, String text) {
        StringBuilder output = new StringBuilder();
        for (char letter : text.toCharArray()) {
            output.append(machine.encrypt(letter));
        }
        return output.toString();
    }
}
