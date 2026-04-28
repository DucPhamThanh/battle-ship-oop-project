package com.battleship;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        
        GameController controller = fxmlLoader.getController();
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, controller::handleKeyPressed);

        stage.setTitle("Battleship - Hải Chiến");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
