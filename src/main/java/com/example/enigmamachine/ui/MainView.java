package com.example.enigmamachine.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class MainView extends BorderPane {

    private static final double SPACING = 12;
    private static final double SIDE_PANEL_WIDTH = 320;

    private final KeyboardView keyboardView = new KeyboardView();

    public MainView(MainController controller) {
        Label rotorWindows = new Label();
        rotorWindows.getStyleClass().add("rotor-windows");
        rotorWindows.textProperty().bind(controller.viewModel().rotorWindowsProperty());

        keyboardView.setOnLetterPressed(controller::pressKey);
        keyboardView.setOnBackspacePressed(controller::deleteLastLetter);
        keyboardView.setOnPastePressed(() -> controller.pasteText(Clipboard.getSystemClipboard().getString()));

        TitledPane rotors = section("Rotors", rotorWindows);
        TitledPane lampboard = section("Lampboard", new LampboardView(controller.viewModel().litLampProperty()));
        TitledPane keyboard = section("Keyboard", keyboardView);

        VBox machine = new VBox(SPACING, rotors, lampboard, keyboard);
        VBox.setVgrow(lampboard, Priority.ALWAYS);
        VBox.setVgrow(keyboard, Priority.ALWAYS);

        TabPane sidePanel = new TabPane(
                tab("Messages", new MessagesView(controller)),
                tab("Settings", new SettingsView(controller)));
        sidePanel.setPrefWidth(SIDE_PANEL_WIDTH);
        setMargin(sidePanel, new Insets(0, 0, 0, SPACING));

        setCenter(machine);
        setRight(sidePanel);
        setPadding(new Insets(SPACING));
    }

    /** Connects the physical keyboard; call after the stage has its scene. */
    public void attachTo(Stage stage) {
        keyboardView.attachTo(stage);
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private static TitledPane section(String title, Node content) {
        TitledPane section = new TitledPane(title, content);
        section.setCollapsible(false);
        section.setMaxHeight(Double.MAX_VALUE);
        return section;
    }
}
