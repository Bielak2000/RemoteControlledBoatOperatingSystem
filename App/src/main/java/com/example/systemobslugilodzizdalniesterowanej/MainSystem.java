package com.example.systemobslugilodzizdalniesterowanej;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jssc.SerialPortException;

import java.io.IOException;

public class MainSystem extends Application {
    Lighting lighting = new Lighting();
    Engines engines = new Engines();
    Flaps flaps = new Flaps();

    public static void main(String[] args) throws SerialPortException {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException, SerialPortException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("window-toolbar-ports.fxml"));
        ChoosePortController choosePortController = new ChoosePortController(stage);
        fxmlLoader.setController(choosePortController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage.setTitle("System obslugi lodzi zdalnie sterowanej");
        stage.setScene(scene);
        stage.show();
        root.requestFocus();
    }
}