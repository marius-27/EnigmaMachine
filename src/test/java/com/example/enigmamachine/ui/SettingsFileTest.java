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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsFileTest {

    private static final MachineConfiguration DEFAULT = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of());
    private static final MachineConfiguration SAVED = new MachineConfiguration(
            List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
            Reflector.C,
            List.of(1, 20, 11),
            List.of(1, 11, 0),
            List.of("AV", "BS"));

    @TempDir
    Path folder;

    private final MainController controller = new MainController(new EnigmaService(DEFAULT));
    private final SettingsViewModel settings = controller.settings();
    private final MachineViewModel viewModel = controller.viewModel();

    @Test
    void savedSettingsCanBeLoadedByAnotherSession() {
        Path file = folder.resolve("machine.json");
        settings.load(SAVED);

        controller.saveSettings(file);

        MainController next = new MainController(new EnigmaService(DEFAULT));
        next.loadSettings(file);
        assertEquals(SAVED, next.settings().toConfiguration());
    }

    @Test
    void saveKeepsTheSettingsShownEvenBeforeTheyAreApplied() {
        Path file = folder.resolve("machine.json");
        settings.load(SAVED);

        controller.saveSettings(file);

        assertEquals("AAA", viewModel.rotorWindowsProperty().get());
        assertTrue(settings.unappliedChangesBinding().get());
    }

    @Test
    void sessionStateIsNotSaved() throws IOException {
        Path file = folder.resolve("machine.json");
        controller.pasteText("HELLO");

        controller.saveSettings(file);

        String json = Files.readString(file);
        assertFalse(json.contains("HELLO"), json);
        assertFalse(json.contains("ILBDA"), json);
        MainController next = new MainController(new EnigmaService(DEFAULT));
        next.loadSettings(file);
        assertEquals(DEFAULT, next.settings().toConfiguration());
    }

    @Test
    void loadedSettingsWaitForApply() {
        Path file = folder.resolve("machine.json");
        settings.load(SAVED);
        controller.saveSettings(file);
        settings.load(DEFAULT);
        controller.pasteText("HELLO");

        controller.loadSettings(file);

        assertEquals(SAVED, settings.toConfiguration());
        assertTrue(settings.unappliedChangesBinding().get());
        assertEquals("HELLO", viewModel.inputTextProperty().get());
        assertEquals("AAF", viewModel.rotorWindowsProperty().get());
    }

    @Test
    void loadedSettingsRunTheMachineAfterApply() {
        Path file = folder.resolve("machine.json");
        settings.load(SAVED);
        controller.saveSettings(file);
        settings.load(DEFAULT);

        controller.loadSettings(file);
        controller.applySettings();
        controller.pasteText("ENIGMA");

        assertEquals(new EnigmaMachine(SAVED).encryptText("ENIGMA"), viewModel.outputTextProperty().get());
    }

    @Test
    void successIsReported() {
        Path file = folder.resolve("machine.json");

        controller.saveSettings(file);
        assertEquals("Saved machine.json.", settings.fileMessageProperty().get());

        controller.loadSettings(file);
        assertEquals("Loaded machine.json.", settings.fileMessageProperty().get());
    }

    @Test
    void unreadableFileIsReportedAndChangesNothing() throws IOException {
        Path file = Files.writeString(folder.resolve("notes.json"), "these are my notes");
        settings.load(SAVED);

        controller.loadSettings(file);

        assertEquals("notes.json is not a valid Enigma configuration file.", settings.fileMessageProperty().get());
        assertEquals(SAVED, settings.toConfiguration());
    }

    @Test
    void missingFileIsReportedAndChangesNothing() {
        settings.load(SAVED);

        controller.loadSettings(folder.resolve("absent.json"));

        assertEquals("The file absent.json does not exist.", settings.fileMessageProperty().get());
        assertEquals(SAVED, settings.toConfiguration());
    }

    @Test
    void failedSaveIsReported() {
        controller.saveSettings(folder.resolve("missing-folder").resolve("machine.json"));

        assertTrue(settings.fileMessageProperty().get().startsWith("Could not save machine.json"));
    }
}
