package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
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
        viewModel.showRotorWindows(service.rotorWindows());
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
            settings.showFileMessage("Saved " + file.getFileName() + ".");
        } catch (ConfigurationFileException e) {
            settings.showFileMessage(e.getMessage());
        }
    }

    /**
     * Fills the editors from a file, reporting the result instead of throwing. The running machine and the
     * current message stay as they are until the settings are applied. A file that cannot be read changes nothing.
     */
    public void loadSettings(Path file) {
        try {
            settings.load(repository.load(file));
            settings.showFileMessage("Loaded " + file.getFileName() + ".");
        } catch (ConfigurationFileException e) {
            settings.showFileMessage(e.getMessage());
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
        process(letters.toString());
    }

    /**
     * Runs the output back through the machine from where the message began, so it becomes the new input.
     * The rotors end where they were, because the same number of letters is processed again.
     */
    public void decrypt() {
        String ciphertext = viewModel.outputTextProperty().get();
        while (service.undoLastLetter()) {
            // Each call moves the rotors one letter closer to the start of the message.
        }
        viewModel.clearMessages();
        process(ciphertext);
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
        viewModel.showRotorWindows(service.rotorWindows());
    }

    /** Removes the last letter from the messages and returns the rotors to where they were before it. */
    public void deleteLastLetter() {
        if (service.undoLastLetter()) {
            viewModel.removeLastLetters();
            viewModel.showRotorWindows(service.rotorWindows());
        }
    }

    /** Empties the message panels and turns the lamp off; the rotors stay where they are. */
    public void clear() {
        service.forgetLetters();
        viewModel.clearMessages();
    }

    /**
     * Starts the machine with the edited settings. The current message was encrypted with the old settings,
     * so it is emptied rather than left next to letters it could no longer be decrypted with.
     */
    public void applySettings() {
        service.configure(settings.toConfiguration());
        settings.markApplied();
        viewModel.clearMessages();
        viewModel.showRotorWindows(service.rotorWindows());
    }

    /** Does what {@link #clear()} does and also returns the rotors to their initial positions. */
    public void reset() {
        service.reset();
        viewModel.clearMessages();
        viewModel.showRotorWindows(service.rotorWindows());
    }
}
