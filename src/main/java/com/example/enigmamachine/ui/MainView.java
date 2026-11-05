package com.example.enigmamachine.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
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
    private final Shortcuts shortcuts = new Shortcuts();

    public MainView(MainController controller) {
        keyboardView.setOnLetterPressed(controller::pressKey);
        keyboardView.setOnBackspacePressed(controller::deleteLastLetter);
        keyboardView.setOnPastePressed(() -> controller.pasteText(Clipboard.getSystemClipboard().getString()));

        TitledPane rotors = section("Rotors", new RotorWindowsView(controller.viewModel().rotorWindowsProperty()));
        TitledPane lampboard = section("Lampboard", new LampboardView(controller.viewModel().litLampProperty()));
        TitledPane keyboard = section("Keyboard", keyboardView);

        VBox machine = new VBox(SPACING, rotors, lampboard, keyboard);
        VBox.setVgrow(lampboard, Priority.ALWAYS);
        VBox.setVgrow(keyboard, Priority.ALWAYS);

        // The settings and the signal path are taller than a small window, so they scroll instead of being cut off.
        TabPane sidePanel = new TabPane(
                tab("Messages", new MessagesView(controller, shortcuts)),
                tab("Settings", scrolling(new SettingsView(controller, shortcuts))),
                tab("Signal", scrolling(new SignalPathView(controller.viewModel().lastTraceProperty(), shortcuts))));
        sidePanel.setPrefWidth(SIDE_PANEL_WIDTH);
        setMargin(sidePanel, new Insets(0, 0, 0, SPACING));
        for (int index = 0; index < sidePanel.getTabs().size(); index++) {
            int tab = index;
            shortcuts.add("Shortcut+" + (index + 1), () -> sidePanel.getSelectionModel().select(tab));
        }

        setCenter(machine);
        setRight(sidePanel);
        setPadding(new Insets(SPACING));
    }

    /** Connects the physical keyboard and the shortcuts; call after the stage has its scene. */
    public void attachTo(Stage stage) {
        keyboardView.attachTo(stage);
        shortcuts.installOn(stage.getScene());
    }

    Shortcuts shortcuts() {
        return shortcuts;
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private static ScrollPane scrolling(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // A focused scroll pane would scroll on Space while a message is being typed.
        scroll.setFocusTraversable(false);
        return scroll;
    }

    private static TitledPane section(String title, Node content) {
        TitledPane section = new TitledPane(title, content);
        section.setCollapsible(false);
        section.setMaxHeight(Double.MAX_VALUE);
        return section;
    }
}
