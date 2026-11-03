package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.engine.EnigmaService;
import com.example.enigmamachine.ui.viewmodel.MachineViewModel;
import java.util.Objects;

public final class MainController {

    private final EnigmaService service;
    private final MachineViewModel viewModel = new MachineViewModel();

    public MainController(EnigmaService service) {
        this.service = Objects.requireNonNull(service, "Enigma service is required.");
        viewModel.showRotorWindows(service.rotorWindows());
    }

    public MachineViewModel viewModel() {
        return viewModel;
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

    /** Empties the message panels and turns the lamp off; the rotors stay where they are. */
    public void clear() {
        viewModel.clearMessages();
    }

    /** Does what {@link #clear()} does and also returns the rotors to their initial positions. */
    public void reset() {
        service.reset();
        viewModel.clearMessages();
        viewModel.showRotorWindows(service.rotorWindows());
    }
}
