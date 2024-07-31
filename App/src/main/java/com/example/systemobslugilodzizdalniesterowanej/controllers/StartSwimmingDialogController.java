package com.example.systemobslugilodzizdalniesterowanej.controllers;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.communication.Connection;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.FXML_RESOURCES_PATH;

public class StartSwimmingDialogController {
    Stage stage;
    BoatModeController boatModeController;
    Connection connection;

    public StartSwimmingDialogController(Stage stage, BoatModeController boatModeController, Connection connection) {
        this.stage = stage;
        this.boatModeController = boatModeController;
        this.connection = connection;
    }

    @FXML
    private Button confirmSwimming;

    @FXML
    private Button noConfirmSwimming;

    @FXML
    void swimming(ActionEvent event) throws IOException, InterruptedException {
        boatModeController.setBoatMode(BoatMode.AUTONOMIC_STARTING);
        Stage stage1 = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_RESOURCES_PATH + "progress-dialog.fxml"));
        ProgressDialogController progressDialogController = new ProgressDialogController(stage1);
        fxmlLoader.setController(progressDialogController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage1.setScene(scene);
        progressDialogController.setDescriptions("Wysyłanie danych", "Trwa wysyłanie danych do łodzi, proszę o cierpliwość.");
        stage1.show();
        root.requestFocus();
        this.stage.close();
        connection.setProgressDialogController(progressDialogController);
        connection.asyncSendChangedBoatModeAndWaypoints();
    }

    @FXML
    void notSwimming(ActionEvent event) {
        this.stage.close();
    }
}
