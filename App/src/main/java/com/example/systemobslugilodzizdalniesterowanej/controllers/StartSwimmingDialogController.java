package com.example.systemobslugilodzizdalniesterowanej.controllers;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.AutonomicController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.LinearAndAngularSpeed;
import com.example.systemobslugilodzizdalniesterowanej.communication.Connection;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.FXML_RESOURCES_PATH;

public class StartSwimmingDialogController {
    private static int MAX_STARTING_BOAT_TIME_SECONDS = 10;
    private final static String BOAT_RUNNING_SWIMMING_INFORMATION = "Łódka porszua się po wyznaczonych punktach. Nie wyłączaj aplikacji i nie wykonuj żadnych czynności, czekaj na informację z łodzi o uzyskaniu docelowej pozycji. Możesz zastopować łódź przyciskiem STOP.";
    Stage stage;
    BoatModeController boatModeController;
    AutonomicController autonomicController;
    Connection connection;
    OSMMap osmMap;
    private Label runningBoatInformation;

    public StartSwimmingDialogController(Stage stage, BoatModeController boatModeController, Connection connection, OSMMap osmMap, AutonomicController autonomicController) {
        this.stage = stage;
        this.boatModeController = boatModeController;
        this.connection = connection;
        this.osmMap = osmMap;
        this.autonomicController = autonomicController;
    }

    @FXML
    private Button confirmSwimming;

    @FXML
    private Button noConfirmSwimming;

    @FXML
    void swimming(ActionEvent event) throws IOException, InterruptedException {
        if (osmMap.getDesignatedWaypoints().size() > 5) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Błędne dane");
            alert.setHeaderText("Wybrałeś zbyt dużo punktów, możesz wybrać maksymalnie 5 waypointów.");
            alert.showAndWait();
            this.stage.close();
        } else if (autonomicController.designateEnginesPower() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Błędne dane");
            alert.setHeaderText("Wybrałeś błędne pozycje.");
            alert.showAndWait();
            this.stage.close();
        } else {
            this.stage.close();
            boatModeController.setBoatMode(BoatMode.AUTONOMIC_STARTING);
            Stage stage1 = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_RESOURCES_PATH + "progress-dialog.fxml"));
            ProgressDialogController progressDialogController = new ProgressDialogController(stage1);
            fxmlLoader.setController(progressDialogController);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            stage1.setScene(scene);
            progressDialogController.setDescriptions("Kalibracja łodzi", "Trwa kalibracja łodzi, proszę o cierpliwość.");
            stage1.show();
            root.requestFocus();
            LinearAndAngularSpeed linearAndAngularSpeed;
            linearAndAngularSpeed = autonomicController.designateRightEnginesPowerOnStart();
            startAndStopRotating(linearAndAngularSpeed);
            linearAndAngularSpeed = autonomicController.designateLeftEnginesPowerOnStart();
            startAndStopRotating(linearAndAngularSpeed);
            boatModeController.setBoatMode(BoatMode.AUTONOMIC_RUNNING);
            Platform.runLater(() -> {
                runningBoatInformation.setVisible(true);
                progressDialogController.closeProgressDialogController();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Łódka rozpoczeła pływanie");
                alert.setHeaderText(BOAT_RUNNING_SWIMMING_INFORMATION);
                alert.getDialogPane().setMaxWidth(700);
                alert.showAndWait();
            });
            linearAndAngularSpeed = autonomicController.designateEnginesPower();
            connection.sendEnginesPowerInAutonomicMode(linearAndAngularSpeed);
        }
    }

    @FXML
    void notSwimming(ActionEvent event) {
        this.stage.close();
    }

    private void startAndStopRotating(LinearAndAngularSpeed linearAndAngularSpeed) throws InterruptedException {
        int waitingIteration = 0;
        designateAndSendEngines(linearAndAngularSpeed);
        autonomicController.setCourseOnRotateStart(osmMap.getCurrentCourse());
        while (!autonomicController.isStopRotating() && waitingIteration < MAX_STARTING_BOAT_TIME_SECONDS) {
            Thread.sleep(1000);
        }
        linearAndAngularSpeed = autonomicController.clearAfterRotating();
        designateAndSendEngines(linearAndAngularSpeed);
        Thread.sleep(1000);
    }

    private void designateAndSendEngines(LinearAndAngularSpeed linearAndAngularSpeed) throws InterruptedException {
        connection.sendEnginesPowerInAutonomicMode(linearAndAngularSpeed);
    }
}
