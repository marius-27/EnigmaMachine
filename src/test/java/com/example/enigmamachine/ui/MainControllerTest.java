package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.EnigmaMachine;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.domain.SignalTrace;
import com.example.enigmamachine.engine.EnigmaService;
import com.example.enigmamachine.ui.viewmodel.MachineViewModel;
import java.util.List;
import org.junit.jupiter.api.Test;

class MainControllerTest {

    private static final MachineConfiguration STARTING_MID_MESSAGE = new MachineConfiguration(
            List.of(RotorDefinition.IV, RotorDefinition.II, RotorDefinition.V),
            Reflector.C,
            List.of(4, 11, 19),
            List.of(16, 3, 24),
            List.of());

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
    void lampShowsTheLastOutputLetter() {
        assertEquals("", viewModel.litLampProperty().get());

        controller.pressKey('A');
        assertEquals("B", viewModel.litLampProperty().get());

        controller.pressKey('A');
        assertEquals("D", viewModel.litLampProperty().get());
    }

    @Test
    void invalidKeyLeavesTheLampAsItWas() {
        controller.pressKey('A');

        controller.pressKey('7');

        assertEquals("B", viewModel.litLampProperty().get());
    }

    @Test
    void clearEmptiesMessagesAndLampButKeepsPositions() {
        controller.pressKey('A');

        controller.clear();

        assertEquals("", viewModel.inputTextProperty().get());
        assertEquals("", viewModel.outputTextProperty().get());
        assertEquals("", viewModel.litLampProperty().get());
        assertEquals("AAB", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void encryptionContinuesFromTheSamePositionsAfterClear() {
        controller.pressKey('A');

        controller.clear();
        controller.pressKey('A');

        assertEquals("D", viewModel.outputTextProperty().get());
    }

    @Test
    void resetTurnsTheLampOff() {
        controller.pressKey('A');

        controller.reset();

        assertEquals("", viewModel.litLampProperty().get());
    }

    @Test
    void everyLetterEncryptsOnceAndStepsOnce() {
        String message = "ENIGMAMACHINE";
        EnigmaMachine reference = new EnigmaMachine(STARTING_MID_MESSAGE);
        MainController typed = new MainController(new EnigmaService(STARTING_MID_MESSAGE));

        for (int typedLetters = 1; typedLetters <= message.length(); typedLetters++) {
            char letter = message.charAt(typedLetters - 1);
            char expected = reference.encrypt(letter);

            typed.pressKey(letter);

            MachineViewModel state = typed.viewModel();
            assertEquals(message.substring(0, typedLetters), state.inputTextProperty().get());
            assertEquals(typedLetters, state.outputTextProperty().get().length());
            assertEquals(String.valueOf(expected), state.litLampProperty().get());
            assertEquals(windowsOf(reference), state.rotorWindowsProperty().get());
        }
    }

    @Test
    void typedMessageMatchesTheEngine() {
        String message = "ENIGMAMACHINE";
        MainController typed = new MainController(new EnigmaService(STARTING_MID_MESSAGE));

        message.chars().forEach(letter -> typed.pressKey((char) letter));

        assertEquals(new EnigmaMachine(STARTING_MID_MESSAGE).encryptText(message),
                typed.viewModel().outputTextProperty().get());
    }

    @Test
    void invalidKeysBetweenLettersDoNotChangeTheResult() {
        MainController typed = new MainController(new EnigmaService(STARTING_MID_MESSAGE));

        typed.pressKey('H');
        typed.pressKey(' ');
        typed.pressKey('7');
        typed.pressKey('I');

        assertEquals(new EnigmaMachine(STARTING_MID_MESSAGE).encryptText("HI"),
                typed.viewModel().outputTextProperty().get());
    }

    @Test
    void doubleStepIsShownInTheRotorWindows() {
        MainController typed = new MainController(new EnigmaService(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 3, 20),
                List.of())));

        typed.pressKey('A');
        assertEquals("ADV", typed.viewModel().rotorWindowsProperty().get());

        typed.pressKey('A');
        assertEquals("AEW", typed.viewModel().rotorWindowsProperty().get());

        typed.pressKey('A');
        assertEquals("BFX", typed.viewModel().rotorWindowsProperty().get());
    }

    @Test
    void resetRestoresNonZeroStartingPositions() {
        MainController typed = new MainController(new EnigmaService(STARTING_MID_MESSAGE));
        String initialWindows = typed.viewModel().rotorWindowsProperty().get();
        typed.pressKey('A');
        typed.pressKey('B');

        typed.reset();

        assertEquals(initialWindows, typed.viewModel().rotorWindowsProperty().get());
    }

    @Test
    void typingTheOutputAfterResetRecoversTheMessage() {
        String message = "ENIGMAMACHINE";
        MainController typed = new MainController(new EnigmaService(STARTING_MID_MESSAGE));
        message.chars().forEach(letter -> typed.pressKey((char) letter));
        String ciphertext = typed.viewModel().outputTextProperty().get();

        typed.reset();
        ciphertext.chars().forEach(letter -> typed.pressKey((char) letter));

        assertEquals(message, typed.viewModel().outputTextProperty().get());
    }

    @Test
    void backspaceRemovesTheLastLetterAndRestoresTheRotors() {
        controller.pressKey('A');
        controller.pressKey('A');

        controller.deleteLastLetter();

        assertEquals("A", viewModel.inputTextProperty().get());
        assertEquals("B", viewModel.outputTextProperty().get());
        assertEquals("B", viewModel.litLampProperty().get());
        assertEquals("AAB", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void backspaceOfTheOnlyLetterTurnsTheLampOff() {
        controller.pressKey('A');

        controller.deleteLastLetter();

        assertEquals("", viewModel.inputTextProperty().get());
        assertEquals("", viewModel.litLampProperty().get());
        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void backspaceOnAnEmptyMessageDoesNothing() {
        controller.deleteLastLetter();

        assertEquals("", viewModel.inputTextProperty().get());
        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void backspaceAfterClearLeavesTheRotorsWhereTheyAre() {
        controller.pressKey('A');
        controller.clear();

        controller.deleteLastLetter();

        assertEquals("AAB", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void backspaceAfterResetDoesNothing() {
        controller.pressKey('A');
        controller.reset();

        controller.deleteLastLetter();

        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void backspaceUndoesADoubleStepAndRetypingGivesTheSameLetters() {
        MainController typed = new MainController(new EnigmaService(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 3, 20),
                List.of())));
        "AAAA".chars().forEach(letter -> typed.pressKey((char) letter));
        String output = typed.viewModel().outputTextProperty().get();

        typed.deleteLastLetter();
        typed.deleteLastLetter();
        assertEquals("AEW", typed.viewModel().rotorWindowsProperty().get());
        typed.deleteLastLetter();
        assertEquals("ADV", typed.viewModel().rotorWindowsProperty().get());

        "AAA".chars().forEach(letter -> typed.pressKey((char) letter));
        assertEquals(output, typed.viewModel().outputTextProperty().get());
    }

    @Test
    void decryptTurnsTheOutputBackIntoTheMessage() {
        controller.pasteText("HELLO");

        controller.decrypt();

        assertEquals("ILBDA", viewModel.inputTextProperty().get());
        assertEquals("HELLO", viewModel.outputTextProperty().get());
        assertEquals("O", viewModel.litLampProperty().get());
        assertEquals("AAF", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void decryptTwiceReturnsToTheCiphertext() {
        controller.pasteText("HELLO");

        controller.decrypt();
        controller.decrypt();

        assertEquals("HELLO", viewModel.inputTextProperty().get());
        assertEquals("ILBDA", viewModel.outputTextProperty().get());
    }

    @Test
    void decryptStartsWhereTheMessageBeganAfterClear() {
        controller.pasteText("ABC");
        controller.clear();
        controller.pasteText("HELLO");

        controller.decrypt();

        assertEquals("HELLO", viewModel.outputTextProperty().get());
        assertEquals("AAI", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void decryptOfAnEmptyMessageChangesNothing() {
        controller.pressKey('A');
        controller.clear();

        controller.decrypt();

        assertEquals("", viewModel.inputTextProperty().get());
        assertEquals("AAB", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void lettersRemainUndoableOneByOneAfterDecrypt() {
        controller.pasteText("HELLO");
        controller.decrypt();

        controller.deleteLastLetter();

        assertEquals("ILBD", viewModel.inputTextProperty().get());
        assertEquals("HELL", viewModel.outputTextProperty().get());
        assertEquals("AAE", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void pastedTextMatchesTypingItsLetters() {
        controller.pasteText("HELLO");

        assertEquals("HELLO", viewModel.inputTextProperty().get());
        assertEquals("ILBDA", viewModel.outputTextProperty().get());
        assertEquals("A", viewModel.litLampProperty().get());
        assertEquals("AAF", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void pastedCiphertextInGroupsDecryptsCorrectly() {
        controller.pasteText("IL BDA\r\n\t");

        assertEquals("HELLO", viewModel.outputTextProperty().get());
    }

    @Test
    void pasteFoldsDiacriticsAndDropsOtherCharacters() {
        controller.pasteText("ț-a,7!");

        assertEquals("TA", viewModel.inputTextProperty().get());
        assertEquals("AAC", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void pasteWithoutLettersChangesNothing() {
        controller.pressKey('A');

        controller.pasteText(" 7! ");
        controller.pasteText("");
        controller.pasteText(null);

        assertEquals("A", viewModel.inputTextProperty().get());
        assertEquals("B", viewModel.litLampProperty().get());
        assertEquals("AAB", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void longPastedTextMatchesTheEngine() {
        String message = "ENIGMA".repeat(2000);

        MainController typed = new MainController(new EnigmaService(STARTING_MID_MESSAGE));
        typed.pasteText(message);

        assertEquals(new EnigmaMachine(STARTING_MID_MESSAGE).encryptText(message),
                typed.viewModel().outputTextProperty().get());
    }

    @Test
    void editingSettingsDoesNotTouchAMessageInProgress() {
        controller.pressKey('A');

        controller.settings().reflectorProperty().set(Reflector.C);
        controller.pressKey('A');

        assertEquals("BD", viewModel.outputTextProperty().get());
        assertEquals("AAC", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void applyingRunsTheMachineWithTheEditedSettings() {
        applyMidMessageSettings();

        controller.pasteText("ENIGMAMACHINE");

        assertEquals(new EnigmaMachine(STARTING_MID_MESSAGE).encryptText("ENIGMAMACHINE"),
                viewModel.outputTextProperty().get());
    }

    @Test
    void applyingEmptiesTheMessageAndShowsTheNewPositions() {
        controller.pressKey('A');

        applyMidMessageSettings();

        assertEquals("", viewModel.inputTextProperty().get());
        assertEquals("", viewModel.outputTextProperty().get());
        assertEquals("", viewModel.litLampProperty().get());
        assertEquals(windowsOf(new EnigmaMachine(STARTING_MID_MESSAGE)), viewModel.rotorWindowsProperty().get());
    }

    @Test
    void lettersFromBeforeApplyingCannotBeUndone() {
        controller.pressKey('A');
        applyMidMessageSettings();
        String windows = viewModel.rotorWindowsProperty().get();

        controller.deleteLastLetter();

        assertEquals(windows, viewModel.rotorWindowsProperty().get());
    }

    @Test
    void resetReturnsToTheAppliedStartPositions() {
        applyMidMessageSettings();
        String windows = viewModel.rotorWindowsProperty().get();
        controller.pressKey('A');

        controller.reset();

        assertEquals(windows, viewModel.rotorWindowsProperty().get());
    }

    @Test
    void sameSettingsGiveTheSameResultAfterApplyingAgain() {
        applyMidMessageSettings();
        controller.pasteText("ENIGMAMACHINE");
        String first = viewModel.outputTextProperty().get();

        controller.applySettings();
        controller.pasteText("ENIGMAMACHINE");

        assertEquals(first, viewModel.outputTextProperty().get());
    }

    @Test
    void decryptAfterApplyingRecoversTheMessage() {
        applyMidMessageSettings();
        controller.pasteText("ENIGMAMACHINE");

        controller.decrypt();

        assertEquals("ENIGMAMACHINE", viewModel.outputTextProperty().get());
    }

    @Test
    void plugboardPairsTakeEffectOnlyAfterApplying() {
        controller.settings().pressPlugboardLetter('A');
        controller.settings().pressPlugboardLetter('M');
        controller.pressKey('A');
        assertEquals("B", viewModel.outputTextProperty().get());

        controller.applySettings();
        controller.pressKey('A');

        assertEquals(1, viewModel.outputTextProperty().get().length());
        assertEquals(new EnigmaMachine(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 0, 0),
                List.of("AM"))).encryptText("A"), viewModel.outputTextProperty().get());
    }

    @Test
    void lastTraceDescribesTheLastLetter() {
        controller.pasteText("HELLO");

        assertEquals('O', viewModel.lastTraceProperty().get().input());
        assertEquals('A', viewModel.lastTraceProperty().get().output());
        assertEquals(List.of(0, 0, 5), viewModel.lastTraceProperty().get().positions());
    }

    @Test
    void lastTraceIsEmptyAtFirstAndAfterEachWayOfRemovingLetters() {
        assertNull(viewModel.lastTraceProperty().get());
        List<Runnable> removals = List.of(
                controller::deleteLastLetter, controller::clear, controller::reset, controller::applySettings);

        for (Runnable removal : removals) {
            controller.pressKey('A');
            removal.run();
            assertNull(viewModel.lastTraceProperty().get());
        }
    }

    @Test
    void lastTraceMatchesWhatTheUserSees() {
        for (char letter : "ENIGMAMACHINE".toCharArray()) {
            controller.pressKey(letter);

            SignalTrace trace = viewModel.lastTraceProperty().get();
            String input = viewModel.inputTextProperty().get();
            String output = viewModel.outputTextProperty().get();
            StringBuilder traceWindows = new StringBuilder();
            trace.positions().forEach(position -> traceWindows.append(Alphabet.toLetter(position)));

            assertEquals(input.charAt(input.length() - 1), trace.input());
            assertEquals(output.charAt(output.length() - 1), trace.steps().getLast().output());
            assertEquals(String.valueOf(trace.output()), viewModel.litLampProperty().get());
            assertEquals(traceWindows.toString(), viewModel.rotorWindowsProperty().get());
        }
    }

    @Test
    void ignoredKeysLeaveTheLastTraceAsItWas() {
        controller.pressKey('A');
        SignalTrace trace = viewModel.lastTraceProperty().get();

        controller.pressKey('7');
        controller.pasteText(" 7! ");

        assertEquals(trace, viewModel.lastTraceProperty().get());
    }

    private void applyMidMessageSettings() {
        for (int slot = 0; slot < 3; slot++) {
            controller.settings().rotorProperty(slot).set(STARTING_MID_MESSAGE.rotors().get(slot));
            controller.settings().positionProperty(slot).set(STARTING_MID_MESSAGE.positions().get(slot));
            controller.settings().ringSettingProperty(slot).set(STARTING_MID_MESSAGE.ringSettings().get(slot));
        }
        controller.settings().reflectorProperty().set(STARTING_MID_MESSAGE.reflector());
        controller.applySettings();
    }

    private static String windowsOf(EnigmaMachine machine) {
        StringBuilder windows = new StringBuilder();
        machine.positions().forEach(position -> windows.append(Alphabet.toLetter(position)));
        return windows.toString();
    }

    @Test
    void requiresService() {
        assertThrows(NullPointerException.class, () -> new MainController(null));
    }
}
