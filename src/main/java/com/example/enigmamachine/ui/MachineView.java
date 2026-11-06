package com.example.enigmamachine.ui;

import javafx.geometry.Insets;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.BorderPane;

/**
 * What the Classic and Diagram views have in common: a keyboard wired to the controller, the side panel with the
 * messages, settings and signal path, and the shortcuts, which are in use only while the view is active.
 */
public abstract class MachineView extends BorderPane implements SwitchableView {

    private final KeyboardView keyboardView = new KeyboardView();
    private final Shortcuts shortcuts = new Shortcuts();
    private final SignalPathView signalPathView;
    private SceneInput input;

    MachineView(MainController controller, String styleClass) {
        getStyleClass().add(styleClass);
        signalPathView = new SignalPathView(
                controller.viewModel().lastTraceProperty(), controller.playbackOptions(), shortcuts);
        keyboardView.setOnLetterPressed(controller::pressKey);
        keyboardView.setOnBackspacePressed(controller::deleteLastLetter);
        keyboardView.setOnPastePressed(() -> controller.pasteText(Clipboard.getSystemClipboard().getString()));

        TabPane sidePanel = ViewParts.sidePanel(controller, shortcuts, signalPathView);
        setMargin(sidePanel, ViewParts.sidePanelMargin());
        setRight(sidePanel);
        setPadding(new Insets(ViewParts.SPACING));
    }

    final KeyboardView keyboardView() {
        return keyboardView;
    }

    final Shortcuts shortcuts() {
        return shortcuts;
    }

    final SignalPathView signalPathView() {
        return signalPathView;
    }

    @Override
    public final void activate(SceneInput sceneInput) {
        if (input != null) {
            return;
        }
        input = sceneInput;
        input.setKeyTarget(keyboardView);
        input.setShortcuts(shortcuts);
        signalPathView.setActive(true);
    }

    @Override
    public final void deactivate() {
        if (input == null) {
            return;
        }
        input.clearKeyTarget(keyboardView);
        input.clearShortcuts(shortcuts);
        input = null;
        signalPathView.setActive(false);
    }
}
