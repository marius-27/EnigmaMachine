package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.enigmamachine.domain.EnigmaMachine;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.engine.EnigmaService;
import com.example.enigmamachine.ui.viewmodel.MachineViewModel;
import com.example.enigmamachine.ui.viewmodel.SettingsViewModel;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingsApplyTest {

    private static final String MESSAGE = "THEXRUSSIANSXAREXCOMINGX";
    private static final MachineConfiguration DEFAULT = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of());

    private final MainController controller = new MainController(new EnigmaService(DEFAULT));
    private final SettingsViewModel settings = controller.settings();
    private final MachineViewModel viewModel = controller.viewModel();

    @Test
    void settingsEnteredInTheEditorDecryptAHistoricalMessage() {
        // Barbarossa message, verified against published sources in HistoricalVectorsTest.
        edit(List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
                Reflector.B, List.of(1, 20, 11), List.of(1, 11, 0));
        press("AVBSCGDLFUHZINKMOWRX");
        controller.applySettings();

        controller.pasteText("EDPUDNRGYSZRCXNUYTPOMRMBOFKTBZREZKMLXLVEFGUEYSIOZVEQMIKUBPMM");

        assertEquals("AUFKLXABTEILUNGXVONXKURTINOWAXKURTINOWAXNORDWESTLXSEBEZXSEBE",
                viewModel.outputTextProperty().get());
    }

    @Test
    void everyRotorOrderAndReflectorMatchesTheEngine() {
        RotorDefinition[] rotors = RotorDefinition.values();
        for (RotorDefinition left : rotors) {
            for (RotorDefinition middle : rotors) {
                for (RotorDefinition right : rotors) {
                    if (left == middle || middle == right || left == right) {
                        continue;
                    }
                    for (Reflector reflector : Reflector.values()) {
                        assertAppliedMatchesEngine(new MachineConfiguration(
                                List.of(left, middle, right), reflector, List.of(0, 0, 0), List.of(0, 0, 0), List.of()));
                    }
                }
            }
        }
    }

    @Test
    void lowestAndHighestPositionsAndRingsMatchTheEngine() {
        for (int lowOrHigh : List.of(0, 25)) {
            assertAppliedMatchesEngine(new MachineConfiguration(
                    DEFAULT.rotors(), Reflector.B, List.of(lowOrHigh, lowOrHigh, lowOrHigh),
                    List.of(lowOrHigh, lowOrHigh, lowOrHigh), List.of()));
        }
    }

    @Test
    void allThirteenPlugboardPairsCanBeApplied() {
        press("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        controller.applySettings();
        controller.pasteText(MESSAGE);
        String ciphertext = viewModel.outputTextProperty().get();
        controller.decrypt();

        assertEquals(new EnigmaMachine(settings.toConfiguration()).encryptText(MESSAGE), ciphertext);
        assertEquals(MESSAGE, viewModel.outputTextProperty().get());
    }

    @Test
    void differentSettingsGiveDifferentCiphertext() {
        controller.pasteText(MESSAGE);
        String withDefault = viewModel.outputTextProperty().get();

        settings.ringSettingProperty(2).set(1);
        controller.applySettings();
        controller.pasteText(MESSAGE);

        assertFalse(withDefault.equals(viewModel.outputTextProperty().get()));
    }

    @Test
    void returningToTheSameSettingsReproducesTheCiphertext() {
        controller.pasteText(MESSAGE);
        String first = viewModel.outputTextProperty().get();

        settings.reflectorProperty().set(Reflector.C);
        controller.applySettings();
        settings.reflectorProperty().set(Reflector.B);
        controller.applySettings();
        controller.pasteText(MESSAGE);

        assertEquals(first, viewModel.outputTextProperty().get());
    }

    @Test
    void resetKeepsSettingsThatAreNotAppliedYet() {
        settings.reflectorProperty().set(Reflector.C);

        controller.reset();

        assertEquals(Reflector.C, settings.reflectorProperty().get());
        assertTrue(settings.unappliedChangesBinding().get());
        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void resetIgnoresPositionsThatAreNotAppliedYet() {
        settings.positionProperty(0).set(7);
        controller.pressKey('A');

        controller.reset();

        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void outOfRangePositionIsRejectedAndTheMachineKeepsRunning() {
        assertRejectedWithoutChangingTheMachine(() -> settings.positionProperty(0).set(26));
    }

    @Test
    void outOfRangeRingSettingIsRejectedAndTheMachineKeepsRunning() {
        assertRejectedWithoutChangingTheMachine(() -> settings.ringSettingProperty(1).set(-1));
    }

    @Test
    void emptyPositionIsRejectedAndTheMachineKeepsRunning() {
        assertRejectedWithoutChangingTheMachine(() -> settings.positionProperty(2).set(null));
    }

    @Test
    void emptyRotorIsRejectedAndTheMachineKeepsRunning() {
        assertRejectedWithoutChangingTheMachine(() -> settings.rotorProperty(0).set(null));
    }

    @Test
    void emptyReflectorIsRejectedAndTheMachineKeepsRunning() {
        assertRejectedWithoutChangingTheMachine(() -> settings.reflectorProperty().set(null));
    }

    @Test
    void applyingReportsTheNewStartPositions() {
        settings.positionProperty(0).set(2);

        controller.applySettings();

        assertEquals("Settings applied. The rotors start at CAA.", settings.messageProperty().get());
    }

    @Test
    void editingASettingEmptiesTheMessageAboutTheOldOnes() {
        settings.positionProperty(0).set(2);
        controller.applySettings();

        settings.reflectorProperty().set(Reflector.C);

        assertEquals("", settings.messageProperty().get());
    }

    @Test
    void plugboardEditsAlsoEmptyTheMessage() {
        settings.showMessage("Saved machine.json.");

        press("AB");

        assertEquals("", settings.messageProperty().get());
    }

    @Test
    void startingPairsAreNormalizedSoTheyCanBeEdited() {
        SettingsViewModel started = new SettingsViewModel(new MachineConfiguration(
                DEFAULT.rotors(), Reflector.B, List.of(0, 0, 0), List.of(0, 0, 0), List.of("zy", "BA")));

        assertEquals(List.of("AB", "YZ"), started.plugboardPairs());
        assertFalse(started.unappliedChangesBinding().get());

        started.pressPlugboardLetter('Y');

        assertEquals(List.of("AB"), started.plugboardPairs());
    }

    private void assertRejectedWithoutChangingTheMachine(Runnable invalidEdit) {
        controller.pressKey('A');

        invalidEdit.run();

        controller.applySettings();

        assertTrue(settings.messageProperty().get().startsWith("Cannot apply these settings: "),
                settings.messageProperty().get());
        assertEquals("A", viewModel.inputTextProperty().get());
        assertEquals("AAB", viewModel.rotorWindowsProperty().get());
        assertTrue(settings.unappliedChangesBinding().get());
        controller.pressKey('A');
        assertEquals("BD", viewModel.outputTextProperty().get());
    }

    private void assertAppliedMatchesEngine(MachineConfiguration configuration) {
        edit(configuration.rotors(), configuration.reflector(), configuration.ringSettings(), configuration.positions());
        controller.applySettings();
        controller.pasteText(MESSAGE);

        assertEquals(new EnigmaMachine(configuration).encryptText(MESSAGE), viewModel.outputTextProperty().get());
    }

    private void edit(
            List<RotorDefinition> rotors, Reflector reflector, List<Integer> ringSettings, List<Integer> positions) {
        for (int slot = 0; slot < rotors.size(); slot++) {
            settings.rotorProperty(slot).set(rotors.get(slot));
            settings.ringSettingProperty(slot).set(ringSettings.get(slot));
            settings.positionProperty(slot).set(positions.get(slot));
        }
        settings.reflectorProperty().set(reflector);
    }

    private void press(String letters) {
        letters.chars().forEach(letter -> settings.pressPlugboardLetter((char) letter));
    }
}
