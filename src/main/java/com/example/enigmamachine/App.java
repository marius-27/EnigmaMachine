package com.example.enigmamachine;

import com.example.enigmamachine.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final double WIDTH = 1100;
    private static final double HEIGHT = 760;
    private static final double MIN_WIDTH = 820;
    private static final double MIN_HEIGHT = 600;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Enigma M3");
        stage.setScene(new Scene(new MainView(), WIDTH, HEIGHT));
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }
}
