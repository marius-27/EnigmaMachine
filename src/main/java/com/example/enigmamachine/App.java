package com.example.enigmamachine;

import com.example.enigmamachine.ui.MainView;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final String STYLESHEET = "/styles/app.css";
    private static final double WIDTH = 1100;
    private static final double HEIGHT = 760;
    private static final double MIN_WIDTH = 820;
    private static final double MIN_HEIGHT = 600;

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new MainView(), WIDTH, HEIGHT);
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource(STYLESHEET)).toExternalForm());

        stage.setTitle("Enigma M3");
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }
}
