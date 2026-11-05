package com.example.enigmamachine.ui;

import java.lang.System.Logger.Level;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;

/**
 * Shows an error that nothing else handled as a readable dialog, instead of leaving only a stack trace
 * in the console. The full details are still logged for whoever investigates the problem.
 */
public final class UnexpectedErrorHandler implements Thread.UncaughtExceptionHandler {

    private static final System.Logger LOGGER = System.getLogger(UnexpectedErrorHandler.class.getName());

    private final Window owner;
    private boolean showing;

    public UnexpectedErrorHandler(Window owner) {
        this.owner = Objects.requireNonNull(owner, "Owner window is required.");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        LOGGER.log(Level.ERROR, "Unexpected error on " + thread.getName(), error);
        if (Platform.isFxApplicationThread()) {
            show(error);
        } else {
            Platform.runLater(() -> show(error));
        }
    }

    private void show(Throwable error) {
        // An error repeated on every frame would otherwise stack up one dialog per frame.
        if (showing) {
            return;
        }
        showing = true;
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Enigma M3");
        alert.setHeaderText("Something went wrong.");
        alert.setContentText(describe(error));
        if (owner.getScene() != null) {
            alert.getDialogPane().getStylesheets().setAll(owner.getScene().getStylesheets());
        }
        alert.setOnHidden(event -> showing = false);
        // show() rather than showAndWait(), which is not allowed while an animation or layout is running.
        alert.show();
    }

    /** A sentence for the dialog: the error's own message when it has one, otherwise its kind. */
    static String describe(Throwable error) {
        String message = error.getMessage();
        String reason = message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
        return reason + "\n\nThe last action did not finish. Check the settings and the message before continuing.";
    }
}
