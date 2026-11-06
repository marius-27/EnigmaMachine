package com.example.enigmamachine.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * The window content: a header with the title, the active mode, the status and the Mode button, above the view of
 * the active mode. Both views share one controller, so switching keeps the messages, the rotor positions, the
 * Undo history and any settings not applied yet; it never encrypts, steps a rotor or applies the settings.
 */
public final class ApplicationShell extends BorderPane {

    private static final InterfaceMode INITIAL_MODE = InterfaceMode.CLASSIC;

    private final Map<InterfaceMode, MachineView> views = new EnumMap<>(InterfaceMode.class);
    private final ReadOnlyObjectWrapper<InterfaceMode> mode = new ReadOnlyObjectWrapper<>(INITIAL_MODE);
    private final Label modeLabel = new Label();
    private final Button modeButton = new Button("Mode");
    private final Shortcuts windowShortcuts = new Shortcuts();
    private final KeyCombination modeKeys;
    private SceneInput input;

    public ApplicationShell(MainController controller) {
        Objects.requireNonNull(controller, "Controller is required.");
        getStyleClass().add("application-shell");
        views.put(InterfaceMode.CLASSIC, new MainView(controller));
        views.put(InterfaceMode.DIAGRAM, new DiagramView(controller));

        Label title = new Label("Enigma M3");
        title.getStyleClass().add("shell-title");
        modeLabel.getStyleClass().add("shell-mode");
        Label status = new Label();
        status.getStyleClass().add("shell-status");
        status.textProperty().bind(controller.viewModel().statusProperty());
        status.setMinWidth(0);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        modeButton.getStyleClass().add("mode-button");
        // A focused button would fire on Space while a message is being typed.
        modeButton.setFocusTraversable(false);
        modeButton.setTooltip(new Tooltip());
        modeButton.setOnAction(event -> switchMode());
        modeKeys = windowShortcuts.add("Shortcut+M", modeButton::fire);

        HBox header = new HBox(ViewParts.SPACING, title, modeLabel, status, gap, modeButton);
        header.getStyleClass().add("shell-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(ViewParts.SPACING, ViewParts.SPACING, 0, ViewParts.SPACING));
        setTop(header);

        showMode(INITIAL_MODE);
    }

    /** Gives the keyboard and the shortcuts of the window to the active view; call once the stage has its scene. */
    public void activate(SceneInput sceneInput) {
        if (input != null) {
            throw new IllegalStateException("The shell is already active.");
        }
        input = Objects.requireNonNull(sceneInput, "Scene input is required.");
        input.setWindowShortcuts(windowShortcuts);
        activeView().activate(input);
    }

    public ReadOnlyObjectProperty<InterfaceMode> modeProperty() {
        return mode.getReadOnlyProperty();
    }

    /** Shows the other mode. The outgoing view lets go of the keyboard and stops its animation first. */
    public void switchMode() {
        activeView().deactivate();
        mode.set(mode.get().other());
        showMode(mode.get());
        if (input != null) {
            activeView().activate(input);
        }
    }

    MachineView activeView() {
        return views.get(mode.get());
    }

    private void showMode(InterfaceMode shown) {
        setCenter(views.get(shown));
        modeLabel.setText(shown.displayName());
        modeLabel.setAccessibleText("Active mode: " + shown.displayName());
        String target = "Switch to " + shown.other().displayName();
        modeButton.getTooltip().setText(target + "\nShortcut: " + modeKeys.getDisplayText());
        modeButton.setAccessibleHelp(modeButton.getTooltip().getText());
    }
}
