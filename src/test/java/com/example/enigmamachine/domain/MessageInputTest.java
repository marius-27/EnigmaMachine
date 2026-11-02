package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class MessageInputTest {

    private static final List<Integer> START = List.of(0, 0, 0);

    @Test
    void emptyMessageProducesEmptyOutputWithoutStepping() {
        EnigmaMachine machine = machine();

        assertEquals("", machine.encryptText(""));
        assertEquals(START, machine.positions());
    }

    @Test
    void dropsSpacesDigitsAndPunctuation() {
        assertEquals(machine().encryptText("ATACLA"), machine().encryptText("ATAC LA 5!"));
    }

    @Test
    void dropsLettersOutsideLatinAlphabet() {
        assertEquals(machine().encryptText("AR"), machine().encryptText("ȚARĂ"));
    }

    @Test
    void droppedCharactersDoNotStepRotors() {
        EnigmaMachine machine = machine();

        assertEquals("", machine.encryptText(" 123 ,.!\n"));
        assertEquals(START, machine.positions());
    }

    @Test
    void stepsOnceForEachAcceptedLetter() {
        EnigmaMachine machine = machine();

        machine.encryptText("A A");

        assertEquals(List.of(0, 0, 2), machine.positions());
    }

    @Test
    void treatsLowerCaseMessageAsUpperCase() {
        assertEquals(machine().encryptText("ENIGMA"), machine().encryptText("enigma"));
    }

    @Test
    void matchesLetterByLetterEncryption() {
        assertEquals("BDZGO", machine().encryptText("AAAAA"));
    }

    @Test
    void singleInvalidLetterIsStillRejected() {
        assertThrows(IllegalArgumentException.class, () -> machine().encrypt(' '));
    }

    @Test
    void rejectsMissingMessage() {
        assertThrows(NullPointerException.class, () -> machine().encryptText(null));
    }

    @Test
    void processesLongMessage() {
        String message = "ENIGMA".repeat(50_000);
        EnigmaMachine machine = machine();

        String ciphertext = machine.encryptText(message);
        machine.reset();

        assertEquals(message.length(), ciphertext.length());
        assertEquals(message, machine.encryptText(ciphertext));
    }

    private static EnigmaMachine machine() {
        return new EnigmaMachine(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                START,
                List.of()));
    }
}
