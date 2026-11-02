package com.example.enigmamachine.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static EnigmaService service(List<Integer> positions) {
        return new EnigmaService(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                positions,
                List.of()));
    }
}
