package com.example.systemobslugilodzizdalniesterowanej;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("progress-dialog.fxml"));
        ProgressDialogController progressDialogController = new ProgressDialogController(stage1);
        fxmlLoader.setController(progressDialogController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage1.setScene(scene);
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
