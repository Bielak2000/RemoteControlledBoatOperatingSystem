package com.example.systemobslugilodzizdalniesterowanej;

import com.example.systemobslugilodzizdalniesterowanej.controllers.ChoosePortController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.FXML_RESOURCES_PATH;

public class MainSystem extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_RESOURCES_PATH + "/window-toolbar-ports.fxml"));
        ChoosePortController choosePortController = new ChoosePortController(stage);
        fxmlLoader.setController(choosePortController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Ustawienia początkowe systemu");
        stage.setScene(scene);
        stage.show();
        root.requestFocus();
    }
}