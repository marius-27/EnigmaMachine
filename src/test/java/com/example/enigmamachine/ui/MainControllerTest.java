package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.engine.EnigmaService;
import com.example.enigmamachine.ui.viewmodel.MachineViewModel;
import java.util.List;
import org.junit.jupiter.api.Test;

class MainControllerTest {

    private final MainController controller = new MainController(new EnigmaService(new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of())));
    private final MachineViewModel viewModel = controller.viewModel();

    @Test
    void startsWithInitialStateAndEmptyMessages() {
        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
        assertEquals("", viewModel.inputTextProperty().get());
        assertEquals("", viewModel.outputTextProperty().get());
    }

    @Test
    void keyPressUpdatesMessagesAndRotorWindows() {
        controller.pressKey('A');
        controller.pressKey('A');

        assertEquals("AA", viewModel.inputTextProperty().get());
        assertEquals("BD", viewModel.outputTextProperty().get());
        assertEquals("AAC", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void lowerCaseKeyIsShownAsUpperCase() {
        controller.pressKey('a');

        assertEquals("A", viewModel.inputTextProperty().get());
        assertEquals("B", viewModel.outputTextProperty().get());
    }

    @Test
    void invalidKeyChangesNothing() {
        controller.pressKey('7');
        controller.pressKey(' ');

        assertEquals("", viewModel.inputTextProperty().get());
        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void resetClearsMessagesAndRestoresPositions() {
        controller.pressKey('A');

        controller.reset();

        assertEquals("", viewModel.inputTextProperty().get());
        assertEquals("", viewModel.outputTextProperty().get());
        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void requiresService() {
        assertThrows(NullPointerException.class, () -> new MainController(null));
    }
}
