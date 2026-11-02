package com.example.enigmamachine.ui;

import javafx.geometry.Insets;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class MainView extends BorderPane {

    private static final double SPACING = 12;
    private static final double MESSAGES_WIDTH = 320;

    public MainView() {
        TitledPane rotors = section("Rotors");
        TitledPane lampboard = section("Lampboard");
        TitledPane keyboard = section("Keyboard");
        TitledPane messages = section("Messages");

        VBox machine = new VBox(SPACING, rotors, lampboard, keyboard);
        VBox.setVgrow(lampboard, Priority.ALWAYS);
        VBox.setVgrow(keyboard, Priority.ALWAYS);

        messages.setPrefWidth(MESSAGES_WIDTH);
        setMargin(messages, new Insets(0, 0, 0, SPACING));

        setCenter(machine);
        setRight(messages);
        setPadding(new Insets(SPACING));
    }

    private static TitledPane section(String title) {
        TitledPane section = new TitledPane(title, new Pane());
        section.setCollapsible(false);
        section.setMaxHeight(Double.MAX_VALUE);
        return section;
    }
}
