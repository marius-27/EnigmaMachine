package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.engine.EnigmaService;
import com.example.enigmamachine.ui.viewmodel.MachineViewModel;
import com.example.enigmamachine.ui.viewmodel.SettingsViewModel;
import java.util.Objects;

public final class MainController {

    private final EnigmaService service;
    private final MachineViewModel viewModel = new MachineViewModel();
    private final SettingsViewModel settings;

    public MainController(EnigmaService service) {
        this.service = Objects.requireNonNull(service, "Enigma service is required.");
        this.settings = new SettingsViewModel(service.configuration());
        viewModel.showRotorWindows(service.rotorWindows());
    }

    public MachineViewModel viewModel() {
        return viewModel;
    }

    public SettingsViewModel settings() {
        return settings;
    }

    public void pressKey(char key) {
        if (!Alphabet.isValid(key)) {
            return;
        }
        char input = Alphabet.normalize(key);
        char output = service.encrypt(input);
        viewModel.appendLetters(input, output);
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

    /** Does what {@link #clear()} does and also returns the rotors to their initial positions. */
    public void reset() {
        service.reset();
        viewModel.clearMessages();
        viewModel.showRotorWindows(service.rotorWindows());
    }
}
