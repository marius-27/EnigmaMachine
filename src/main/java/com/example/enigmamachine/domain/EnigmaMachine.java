package com.example.enigmamachine.domain;

import com.example.enigmamachine.domain.SignalTrace.Stage;
import com.example.enigmamachine.domain.SignalTrace.Step;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

public final class EnigmaMachine {

    private static final int LEFT = 0;
    private static final int MIDDLE = 1;
    private static final int RIGHT = 2;

    private final MachineConfiguration configuration;
    private final Plugboard plugboard;
    private Rotor left;
    private Rotor middle;
    private Rotor right;

    public EnigmaMachine(MachineConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Machine configuration is required.");
        this.plugboard = new Plugboard(configuration.plugboardPairs());
        reset();
    }

    public void reset() {
        restorePositions(configuration.positions());
    }

    /**
     * Puts the rotors back to positions read earlier from {@link #positions()}.
     */
    public void restorePositions(List<Integer> positions) {
        if (positions.size() != configuration.rotors().size()) {
            throw new IllegalArgumentException("Expected 3 positions, got " + positions.size() + ".");
        }
        left = createRotor(LEFT, positions.get(LEFT));
        middle = createRotor(MIDDLE, positions.get(MIDDLE));
        right = createRotor(RIGHT, positions.get(RIGHT));
    }

    public List<Integer> positions() {
        return List.of(left.position(), middle.position(), right.position());
    }

    public char encrypt(char letter) {
        return encryptTraced(letter).output();
    }

    /**
     * Encrypts a letter like {@link #encrypt(char)} and also reports every component the signal crossed.
     */
    public SignalTrace encryptTraced(char letter) {
        int input = Alphabet.toIndex(letter);
        SteppingMechanism.step(left, middle, right);
        List<Integer> positions = positions();

        List<Step> steps = new ArrayList<>(Stage.values().length);
        int signal = pass(steps, Stage.PLUGBOARD_IN, input, plugboard::swap);
        signal = pass(steps, Stage.RIGHT_ROTOR_FORWARD, signal, right::forward);
        signal = pass(steps, Stage.MIDDLE_ROTOR_FORWARD, signal, middle::forward);
        signal = pass(steps, Stage.LEFT_ROTOR_FORWARD, signal, left::forward);
        signal = pass(steps, Stage.REFLECTOR, signal, configuration.reflector()::reflect);
        signal = pass(steps, Stage.LEFT_ROTOR_REVERSE, signal, left::reverse);
        signal = pass(steps, Stage.MIDDLE_ROTOR_REVERSE, signal, middle::reverse);
        signal = pass(steps, Stage.RIGHT_ROTOR_REVERSE, signal, right::reverse);
        signal = pass(steps, Stage.PLUGBOARD_OUT, signal, plugboard::swap);

        return new SignalTrace(Alphabet.toLetter(input), Alphabet.toLetter(signal), positions, steps);
    }

    private static int pass(List<Step> steps, Stage stage, int input, IntUnaryOperator component) {
        int output = component.applyAsInt(input);
        steps.add(new Step(stage, Alphabet.toLetter(input), Alphabet.toLetter(output)));
        return output;
    }

    /**
     * Encrypts the letters of a message. Other characters are dropped and do not step the rotors.
     */
    public String encryptText(String text) {
        StringBuilder output = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            if (Alphabet.isValid(character)) {
                output.append(encrypt(character));
            }
        }
        return output.toString();
    }

    private Rotor createRotor(int slot, int position) {
        return new Rotor(configuration.rotors().get(slot), configuration.ringSettings().get(slot), position);
    }
}
