package com.example.enigmamachine;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.engine.EnigmaService;
import com.example.enigmamachine.ui.ApplicationShell;
import com.example.enigmamachine.ui.MainController;
import com.example.enigmamachine.ui.SceneInput;
import com.example.enigmamachine.ui.UnexpectedErrorHandler;
import java.util.List;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final String STYLESHEET = "/styles/app.css";
    private static final double WIDTH = 1100;
    private static final double HEIGHT = 760;
    private static final double MIN_WIDTH = 860;
    private static final double MIN_HEIGHT = 600;
    private static final MachineConfiguration DEFAULT_CONFIGURATION = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of());

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(new EnigmaService(DEFAULT_CONFIGURATION));

        ApplicationShell shell = new ApplicationShell(controller);
        Scene scene = new Scene(shell, WIDTH, HEIGHT);
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource(STYLESHEET)).toExternalForm());

        Thread.setDefaultUncaughtExceptionHandler(new UnexpectedErrorHandler(stage));
        stage.setTitle("Enigma M3");
        stage.setScene(scene);
        shell.activate(SceneInput.installOn(stage));
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }
}
