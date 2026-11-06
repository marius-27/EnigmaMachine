package com.example.enigmamachine.engine;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.EnigmaMachine;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.SignalTrace;
import com.example.enigmamachine.domain.WiringSnapshot;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class EnigmaService {

    private MachineConfiguration configuration;
    private EnigmaMachine machine;
    // Double stepping cannot be run backwards from a position alone, so each letter records where the rotors were.
    private final Deque<List<Integer>> positionsBeforeLetters = new ArrayDeque<>();

    public EnigmaService(MachineConfiguration configuration) {
        configure(configuration);
    }

    public MachineConfiguration configuration() {
        return configuration;
    }

    /** Replaces the machine with one built from the given settings, at its start positions. */
    public void configure(MachineConfiguration newConfiguration) {
        machine = new EnigmaMachine(newConfiguration);
        configuration = newConfiguration;
        positionsBeforeLetters.clear();
    }

    public char encrypt(char letter) {
        return encryptTraced(letter).output();
    }

    /** Encrypts a letter like {@link #encrypt(char)} and also reports the path of its signal. */
    public SignalTrace encryptTraced(char letter) {
        List<Integer> positionsBefore = machine.positions();
        SignalTrace trace = machine.encryptTraced(letter);
        positionsBeforeLetters.push(positionsBefore);
        return trace;
    }

    /** Returns the rotors to where they were before the last letter, or returns false if there is none. */
    public boolean undoLastLetter() {
        if (positionsBeforeLetters.isEmpty()) {
            return false;
        }
        machine.restorePositions(positionsBeforeLetters.pop());
        return true;
    }

    /** Keeps the rotors where they are, but the letters so far can no longer be undone. */
    public void forgetLetters() {
        positionsBeforeLetters.clear();
    }

    /** True while the rotors are at the start positions of the current settings. */
    public boolean atStartPositions() {
        return machine.positions().equals(configuration.positions());
    }

    public void reset() {
        machine.reset();
        positionsBeforeLetters.clear();
    }

    /** Where every contact of the running machine leads right now. */
    public WiringSnapshot wiringSnapshot() {
        return machine.wiringSnapshot();
    }

    public String rotorWindows() {
        StringBuilder windows = new StringBuilder();
        for (int position : machine.positions()) {
            windows.append(Alphabet.toLetter(position));
        }
        return windows.toString();
    }
}
