package com.battleship;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.io.IOException;
import com.battleship.logic.BotAI;


public class MenuController {

    @FXML
    private void onStartEasy(ActionEvent event) throws IOException {
        startGame(event, BotAI.Difficulty.EASY);
    }

    @FXML
    private void onStartMedium(ActionEvent event) throws IOException {
        startGame(event, BotAI.Difficulty.MEDIUM);
    }

    @FXML
    private void onStartHard(ActionEvent event) throws IOException {
        startGame(event, BotAI.Difficulty.HARD);
    }

    private void startGame(ActionEvent event, BotAI.Difficulty difficulty) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

        GameController controller = fxmlLoader.getController();
        controller.setDifficulty(difficulty);

        stage.setScene(scene);
    }

    @FXML
    private void onShowInstructions(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("instructions-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        stage.setScene(scene);
    }

    @FXML
    private void onBackToMenu(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("menu-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        stage.setScene(scene);
    }
}
