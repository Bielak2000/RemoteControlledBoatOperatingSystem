package com.example.systemobslugilodzizdalniesterowanej;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

public class StartSwimmingDialogController {
    Stage stage;
    BoatModeController boatModeController;
    Connection connection;
    ToggleButton modeChooser;
    Button startSwimming;
    Button clearTrace;
    Button exit;

    public StartSwimmingDialogController(Stage stage, BoatModeController boatModeController, Connection connection,
                                         ToggleButton modeChooser, Button startSwimming, Button clearTrace, Button exit) {
        this.stage = stage;
        this.boatModeController = boatModeController;
        this.connection = connection;
        this.modeChooser = modeChooser;
        this.startSwimming = startSwimming;
        this.clearTrace = clearTrace;
        this.exit = exit;
    }

    @FXML
    private Button confirmSwimming;

    @FXML
    private Button noConfirmSwimming;

    @FXML
    void swimming(ActionEvent event) throws IOException, InterruptedException {
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
//        blockActionsForAutonomicRunningBoatMode();
//        boatModeController.setBoatMode(BoatMode.AUTONOMIC_RUNNING);
//        connection.sendChangedBoatModeAndWaypoints();
        Thread.sleep(3000);
        progressDialogController.closeProgressDialogController();
    }

    @FXML
    void notSwimming(ActionEvent event) {
        this.stage.close();
    }

    private void blockActionsForAutonomicRunningBoatMode() {
        modeChooser.setDisable(true);
        startSwimming.setDisable(true);
        clearTrace.setDisable(true);
        exit.setDisable(true);
    }
}
