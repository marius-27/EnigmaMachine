package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.SignalTrace;
import com.example.enigmamachine.engine.EnigmaService;
import com.example.enigmamachine.persistence.ConfigurationFileException;
import com.example.enigmamachine.persistence.ConfigurationRepository;
import com.example.enigmamachine.persistence.JsonConfigurationRepository;
import com.example.enigmamachine.ui.viewmodel.MachineViewModel;
import com.example.enigmamachine.ui.viewmodel.SettingsViewModel;
import java.nio.file.Path;
import java.util.Objects;

public final class MainController {

    private final EnigmaService service;
    private final MachineViewModel viewModel = new MachineViewModel();
    private final SettingsViewModel settings;
    private final ConfigurationRepository repository;

    public MainController(EnigmaService service) {
        this(service, new JsonConfigurationRepository());
    }

    public MainController(EnigmaService service, ConfigurationRepository repository) {
        this.service = Objects.requireNonNull(service, "Enigma service is required.");
        this.repository = Objects.requireNonNull(repository, "Configuration repository is required.");
        this.settings = new SettingsViewModel(service.configuration());
        showMachineState();
    }

    public MachineViewModel viewModel() {
        return viewModel;
    }

    public SettingsViewModel settings() {
        return settings;
    }

    /** Saves the settings shown in the editors, reporting the result instead of throwing. */
    public void saveSettings(Path file) {
        try {
            repository.save(settings.toConfiguration(), file);
            settings.showMessage("Saved " + file.getFileName() + ".");
        } catch (ConfigurationFileException e) {
            settings.showMessage(e.getMessage());
        } catch (IllegalArgumentException | NullPointerException e) {
            settings.showMessage("Cannot save these settings: " + e.getMessage());
        }
    }

    /**
     * Fills the editors from a file, reporting the result instead of throwing. The running machine and the
     * current message stay as they are until the settings are applied. A file that cannot be read changes nothing.
     */
    public void loadSettings(Path file) {
        try {
            settings.load(repository.load(file));
            settings.showMessage("Loaded " + file.getFileName() + ". Apply to use these settings.");
        } catch (ConfigurationFileException e) {
            settings.showMessage(e.getMessage());
        }
    }

    public void pressKey(char key) {
        if (!Alphabet.isValid(key)) {
            return;
        }
        process(String.valueOf(Alphabet.normalize(key)));
    }

    /**
     * Processes pasted text from the current rotor positions. Whitespace is skipped rather than typed as X,
     * because ciphertext is often written in groups of letters and an extra X would corrupt its decryption.
     */
    public void pasteText(String text) {
        if (text == null) {
            return;
        }
        StringBuilder letters = new StringBuilder(text.length());
        for (char typed : text.toCharArray()) {
            if (!Character.isWhitespace(typed)) {
                LetterFolding.toLetter(typed).ifPresent(letters::append);
            }
        }
        if (letters.isEmpty()) {
            viewModel.showStatus("The pasted text has no letters to encrypt.");
            return;
        }
        process(letters.toString());
    }

    /**
     * Runs the output back through the machine from where the message began, so it becomes the new input.
     * The rotors end where they were, because the same number of letters is processed again.
     */
    public void decrypt() {
        String ciphertext = viewModel.outputTextProperty().get();
        if (ciphertext.isEmpty()) {
            return;
        }
        while (service.undoLastLetter()) {
            // Each call moves the rotors one letter closer to the start of the message.
        }
        viewModel.clearMessages();
        process(ciphertext);
        viewModel.showStatus("Decrypted " + letterCount(ciphertext.length()) + ".");
    }

    private void process(String letters) {
        if (letters.isEmpty()) {
            return;
        }
        StringBuilder output = new StringBuilder(letters.length());
        SignalTrace lastTrace = null;
        for (char letter : letters.toCharArray()) {
            lastTrace = service.encryptTraced(letter);
            output.append(lastTrace.output());
        }
        viewModel.appendLetters(letters, output.toString());
        viewModel.showLastTrace(lastTrace);
        viewModel.showStatus("");
        showMachineState();
    }

    /** Removes the last letter from the messages and returns the rotors to where they were before it. */
    public void deleteLastLetter() {
        if (service.undoLastLetter()) {
            viewModel.removeLastLetters();
            viewModel.showStatus("");
            showMachineState();
        }
    }

    /** Empties the message panels and turns the lamp off; the rotors stay where they are. */
    public void clear() {
        service.forgetLetters();
        viewModel.clearMessages();
        viewModel.showStatus("Message cleared. The rotors stay at " + service.rotorWindows() + ".");
        showMachineState();
    }

    /**
     * Starts the machine with the edited settings. The current message was encrypted with the old settings,
     * so it is emptied rather than left next to letters it could no longer be decrypted with.
     * Settings the machine cannot run with are reported, and the machine and message stay as they were.
     */
    public void applySettings() {
        MachineConfiguration configuration;
        try {
            configuration = settings.toConfiguration();
            service.configure(configuration);
        } catch (IllegalArgumentException | NullPointerException e) {
            settings.showMessage("Cannot apply these settings: " + e.getMessage());
            return;
        }
        settings.markApplied();
        viewModel.clearMessages();
        viewModel.showStatus("New settings applied.");
        showMachineState();
        settings.showMessage("Settings applied. The rotors start at " + service.rotorWindows() + ".");
    }

    /** Does what {@link #clear()} does and also returns the rotors to their initial positions. */
    public void reset() {
        service.reset();
        viewModel.clearMessages();
        viewModel.showStatus("Reset. The rotors are back at " + service.rotorWindows() + ".");
        showMachineState();
    }

    private void showMachineState() {
        viewModel.showRotorWindows(service.rotorWindows());
        viewModel.showResettable(
                !service.atStartPositions() || !viewModel.inputTextProperty().get().isEmpty());
    }

    private static String letterCount(int count) {
        return count == 1 ? "1 letter" : count + " letters";
    }
}
