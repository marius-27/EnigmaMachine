package com.example.enigmamachine.ui;

import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** The classic interface: rotor windows, lampboard and keyboard, with the side panel next to them. */
public final class MainView extends MachineView {

    public MainView(MainController controller) {
        super(controller, "classic-view");
        TitledPane rotors = ViewParts.section(
                "Rotors", new RotorWindowsView(controller.viewModel().rotorWindowsProperty()));
        TitledPane lampboard = ViewParts.section(
                "Lampboard", new LampboardView(controller.viewModel().litLampProperty()));
        TitledPane keyboard = ViewParts.section("Keyboard", keyboardView());

        VBox machine = new VBox(ViewParts.SPACING, rotors, lampboard, keyboard);
        VBox.setVgrow(lampboard, Priority.ALWAYS);
        VBox.setVgrow(keyboard, Priority.ALWAYS);
        setCenter(machine);
    }
}
