package com.example.enigmamachine.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnigmaServiceTest {

    @Test
    void showsInitialPositionsAsLetters() {
        assertEquals("BLA", service(List.of(1, 11, 0)).rotorWindows());
    }

    @Test
    void encryptsThroughMachineAndUpdatesWindows() {
        EnigmaService service = service(List.of(0, 0, 0));

        assertEquals('B', service.encrypt('A'));
        assertEquals("AAB", service.rotorWindows());
    }

    @Test
    void resetRestoresInitialWindows() {
        EnigmaService service = service(List.of(0, 0, 0));
        service.encrypt('A');

        service.reset();

        assertEquals("AAA", service.rotorWindows());
    }

    @Test
    void undoReturnsTheRotorsToBeforeTheLastLetter() {
        EnigmaService service = service(List.of(0, 0, 0));
        service.encrypt('A');
        service.encrypt('A');

        assertTrue(service.undoLastLetter());

        assertEquals("AAB", service.rotorWindows());
    }

    @Test
    void undoWithNoLettersDoesNothing() {
        EnigmaService service = service(List.of(0, 0, 0));

        assertFalse(service.undoLastLetter());
        assertEquals("AAA", service.rotorWindows());
    }

    @Test
    void rejectedLetterIsNotRecordedForUndo() {
        EnigmaService service = service(List.of(0, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> service.encrypt('7'));

        assertFalse(service.undoLastLetter());
    }

    @Test
    void resetEndsTheUndoHistory() {
        EnigmaService service = service(List.of(0, 0, 0));
        service.encrypt('A');

        service.reset();

        assertFalse(service.undoLastLetter());
    }

    @Test
    void forgetLettersEndsTheUndoHistoryAndKeepsTheRotors() {
        EnigmaService service = service(List.of(0, 0, 0));
        service.encrypt('A');

        service.forgetLetters();

        assertFalse(service.undoLastLetter());
        assertEquals("AAB", service.rotorWindows());
    }

    @Test
    void configureStartsAtTheNewPositionsAndForgetsLetters() {
        EnigmaService service = service(List.of(0, 0, 0));
        service.encrypt('A');
        MachineConfiguration next = service(List.of(1, 11, 0)).configuration();

        service.configure(next);

        assertEquals(next, service.configuration());
        assertEquals("BLA", service.rotorWindows());
        assertFalse(service.undoLastLetter());
    }

    @Test
    void failedConfigureLeavesTheMachineRunning() {
        EnigmaService service = service(List.of(0, 0, 0));
        MachineConfiguration running = service.configuration();

        assertThrows(NullPointerException.class, () -> service.configure(null));

        assertEquals(running, service.configuration());
        assertEquals('B', service.encrypt('A'));
    }

    @Test
    void tracedLetterEncryptsAndCanBeUndoneLikeAnyOther() {
        EnigmaService service = service(List.of(0, 0, 0));

        assertEquals('B', service.encryptTraced('A').output());
        assertEquals("AAB", service.rotorWindows());

        assertTrue(service.undoLastLetter());
        assertEquals("AAA", service.rotorWindows());
    }

    private static EnigmaService service(List<Integer> positions) {
        return new EnigmaService(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                positions,
                List.of()));
    }
}
