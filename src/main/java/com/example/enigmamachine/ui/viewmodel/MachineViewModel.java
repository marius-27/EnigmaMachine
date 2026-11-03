package com.example.enigmamachine.ui.viewmodel;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public final class MachineViewModel {

    private final ReadOnlyStringWrapper rotorWindows = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper inputText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper outputText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper litLamp = new ReadOnlyStringWrapper("");

    public ReadOnlyStringProperty rotorWindowsProperty() {
        return rotorWindows.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty inputTextProperty() {
        return inputText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty outputTextProperty() {
        return outputText.getReadOnlyProperty();
    }

    /** The letter of the lamp that is on, or an empty string when all lamps are off. */
    public ReadOnlyStringProperty litLampProperty() {
        return litLamp.getReadOnlyProperty();
    }

    public void showRotorWindows(String windows) {
        rotorWindows.set(windows);
    }

    public void appendLetters(char input, char output) {
        inputText.set(inputText.get() + input);
        outputText.set(outputText.get() + output);
        litLamp.set(String.valueOf(output));
    }

    public void removeLastLetters() {
        inputText.set(withoutLastLetter(inputText.get()));
        outputText.set(withoutLastLetter(outputText.get()));
        String output = outputText.get();
        litLamp.set(output.isEmpty() ? "" : output.substring(output.length() - 1));
    }

    public void clearMessages() {
        inputText.set("");
        outputText.set("");
        litLamp.set("");
    }

    private static String withoutLastLetter(String text) {
        return text.substring(0, text.length() - 1);
    }
}
