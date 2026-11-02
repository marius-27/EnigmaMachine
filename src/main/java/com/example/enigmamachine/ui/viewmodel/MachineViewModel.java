package com.example.enigmamachine.ui.viewmodel;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public final class MachineViewModel {

    private final ReadOnlyStringWrapper rotorWindows = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper inputText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper outputText = new ReadOnlyStringWrapper("");

    public ReadOnlyStringProperty rotorWindowsProperty() {
        return rotorWindows.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty inputTextProperty() {
        return inputText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty outputTextProperty() {
        return outputText.getReadOnlyProperty();
    }

    public void showRotorWindows(String windows) {
        rotorWindows.set(windows);
    }

    public void appendLetters(char input, char output) {
        inputText.set(inputText.get() + input);
        outputText.set(outputText.get() + output);
    }

    public void clearMessages() {
        inputText.set("");
        outputText.set("");
    }
}
