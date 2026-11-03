package com.example.enigmamachine.ui;

import com.example.enigmamachine.ui.viewmodel.SettingsViewModel;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Letter sockets for pairing the plugboard by clicking, with the list of connections below them.
 */
public final class PlugboardView extends VBox {

    private static final double SPACING = 4;
    private static final PseudoClass CONNECTED = PseudoClass.getPseudoClass("connected");
    private static final PseudoClass PENDING = PseudoClass.getPseudoClass("pending");

    private final SettingsViewModel settings;
    private final Map<Character, Button> sockets = new HashMap<>();

    public PlugboardView(SettingsViewModel settings) {
        super(SPACING);
        this.settings = settings;

        getChildren().add(new Label("Plugboard"));
        for (String row : KeyboardView.ROWS) {
            HBox rowBox = new HBox(SPACING);
            rowBox.setAlignment(Pos.CENTER);
            for (char letter : row.toCharArray()) {
                rowBox.getChildren().add(createSocket(letter));
            }
            getChildren().add(rowBox);
        }

        Label connections = new Label();
        connections.getStyleClass().add("plugboard-connections");
        connections.setWrapText(true);
        connections.textProperty().bind(Bindings.createStringBinding(
                () -> settings.plugboardPairs().isEmpty()
                        ? "No connections"
                        : String.join(" ", settings.plugboardPairs()),
                settings.plugboardPairs()));

        Label warning = new Label();
        warning.getStyleClass().add("plugboard-warning");
        warning.setWrapText(true);
        warning.textProperty().bind(settings.plugboardWarningProperty());
        warning.managedProperty().bind(warning.textProperty().isNotEmpty());
        getChildren().addAll(connections, warning);

        settings.plugboardPairs().addListener((ListChangeListener<String>) change -> refreshSockets());
        settings.pendingPlugboardLetterProperty().addListener((observable, previous, current) -> refreshSockets());
        refreshSockets();
    }

    private Button createSocket(char letter) {
        Button socket = new Button(String.valueOf(letter));
        socket.getStyleClass().add("plugboard-socket");
        // A focused socket would fire on Space while a message is being typed.
        socket.setFocusTraversable(false);
        socket.setOnAction(event -> settings.pressPlugboardLetter(letter));
        sockets.put(letter, socket);
        return socket;
    }

    private void refreshSockets() {
        String connected = String.join("", settings.plugboardPairs());
        String pending = settings.pendingPlugboardLetterProperty().get();
        sockets.forEach((letter, socket) -> {
            socket.pseudoClassStateChanged(CONNECTED, connected.indexOf(letter) >= 0);
            socket.pseudoClassStateChanged(PENDING, pending.indexOf(letter) >= 0);
        });
    }
}
