package com.example.enigmamachine.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class MainView extends BorderPane {

    private static final double SPACING = 12;
    private static final double MESSAGES_WIDTH = 320;

    private final KeyboardView keyboardView = new KeyboardView();

    public MainView(MainController controller) {
        Label rotorWindows = new Label();
        rotorWindows.getStyleClass().add("rotor-windows");
        rotorWindows.textProperty().bind(controller.viewModel().rotorWindowsProperty());

        TitledPane rotors = section("Rotors", rotorWindows);
        TitledPane lampboard = section("Lampboard", new Pane());
        TitledPane keyboard = section("Keyboard", keyboardView);
        TitledPane messages = section("Messages", new Pane());

        VBox machine = new VBox(SPACING, rotors, lampboard, keyboard);
        VBox.setVgrow(lampboard, Priority.ALWAYS);
        VBox.setVgrow(keyboard, Priority.ALWAYS);

        messages.setPrefWidth(MESSAGES_WIDTH);
        setMargin(messages, new Insets(0, 0, 0, SPACING));

        setCenter(machine);
        setRight(messages);
        setPadding(new Insets(SPACING));
    }

    /** Connects the physical keyboard; call after the stage has its scene. */
    public void attachTo(Stage stage) {
        keyboardView.attachTo(stage);
    }

    private static TitledPane section(String title, Node content) {
        TitledPane section = new TitledPane(title, content);
        section.setCollapsible(false);
        section.setMaxHeight(Double.MAX_VALUE);
        return section;
    }
}
