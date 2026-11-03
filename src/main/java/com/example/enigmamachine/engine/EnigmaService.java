package com.example.enigmamachine.engine;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.EnigmaMachine;
import com.example.enigmamachine.domain.MachineConfiguration;

public final class EnigmaService {

    private final MachineConfiguration configuration;
    private final EnigmaMachine machine;

    public EnigmaService(MachineConfiguration configuration) {
        this.configuration = configuration;
        this.machine = new EnigmaMachine(configuration);
    }

    public MachineConfiguration configuration() {
        return configuration;
    }

    public char encrypt(char letter) {
        return machine.encrypt(letter);
    }

    public void reset() {
        machine.reset();
    }

    public String rotorWindows() {
        StringBuilder windows = new StringBuilder();
        for (int position : machine.positions()) {
            windows.append(Alphabet.toLetter(position));
        }
        return windows.toString();
    }
}
