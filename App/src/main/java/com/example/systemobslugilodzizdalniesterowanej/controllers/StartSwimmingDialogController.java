package com.example.systemobslugilodzizdalniesterowanej.controllers;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.AutonomicController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.LinearAndAngularSpeed;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.PositionAlgorithm;
import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.FXML_RESOURCES_PATH;

@Slf4j
public class StartSwimmingDialogController {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    private final static String BOAT_RUNNING_SWIMMING_INFORMATION = "Łódka porszua się po wyznaczonych punktach. Nie wyłączaj aplikacji i nie wykonuj żadnych czynności, czekaj na informację z łodzi o uzyskaniu docelowej pozycji. Możesz zastopować łódź przyciskiem STOP.";
    Stage stage;
    BoatModeController boatModeController;
    AutonomicController autonomicController;
    Connection connection;
    OSMMap osmMap;
    private Label runningBoatInformation;
    private PositionAlgorithm chosenAlgorithm;

    public StartSwimmingDialogController(PositionAlgorithm chosenAlgorithm, Stage stage, BoatModeController boatModeController, Connection connection, OSMMap osmMap, AutonomicController autonomicController, Label runningBoatInformation) {
        this.stage = stage;
        this.boatModeController = boatModeController;
        this.connection = connection;
        this.osmMap = osmMap;
        this.autonomicController = autonomicController;
        this.runningBoatInformation = runningBoatInformation;
        this.chosenAlgorithm = chosenAlgorithm;
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
        } else if (autonomicController.designateSpeeds() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Błędne dane");
            alert.setHeaderText("Wybrałeś błędne pozycje.");
            alert.showAndWait();
            this.stage.close();
        } else {
            this.stage.close();
            boatModeController.setBoatMode(BoatMode.AUTONOMIC_STARTING);
            ProgressDialogController progressDialogController;
            Stage stage1 = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_RESOURCES_PATH + "progress-dialog.fxml"));
            progressDialogController = new ProgressDialogController(stage1);
            fxmlLoader.setController(progressDialogController);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            stage1.setScene(scene);
            progressDialogController.setDescriptions("Rozpoczęto kalibrację", "Trwa kalibracja łodzi, proszę o cierpliwość ...");
            stage1.show();
            root.requestFocus();
            executor.submit(() -> {
                if (chosenAlgorithm != PositionAlgorithm.ONLY_GPS) {
                    LinearAndAngularSpeed linearAndAngularSpeed;
                    linearAndAngularSpeed = autonomicController.designateRightEnginesPowerOnStart();
                    try {
                        connection.startAndStopRotating(linearAndAngularSpeed, osmMap.getCurrentCourse());
                    } catch (InterruptedException e) {
                        log.error("Error while startAndStopRotatnig: {}", e.getMessage());
                    }
                    linearAndAngularSpeed = autonomicController.designateLeftEnginesPowerOnStart();
                    try {
                        connection.startAndStopRotating(linearAndAngularSpeed, osmMap.getCurrentCourse());
                    } catch (InterruptedException e) {
                        log.error("Error while startAndStopRotatnig: {}", e.getMessage());
                    }

//                    connection.lineUpTowardsTheTarget();
                }
                Platform.runLater(() -> {
                    runningBoatInformation.setVisible(true);
                    progressDialogController.closeProgressDialogController();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Łódka rozpoczeła pływanie");
                    alert.setHeaderText(BOAT_RUNNING_SWIMMING_INFORMATION);
                    alert.getDialogPane().setMaxWidth(700);
                    alert.show();
                    boatModeController.setBoatMode(BoatMode.AUTONOMIC_RUNNING);
                });
            });
            executor.shutdown();
        }
    }


    @FXML
    void notSwimming(ActionEvent event) {
        this.stage.close();
    }

    private void startAndStopRotating(LinearAndAngularSpeed linearAndAngularSpeed, double courseToEnd) throws InterruptedException {
        int waitingIteration = 0;
        autonomicController.setStopRotating(false);
        connection.sendEnginesPowerInAutonomicMode(linearAndAngularSpeed);
        autonomicController.setCourseOnRotateStart(courseToEnd);
        while (!autonomicController.isStopRotating() && waitingIteration < Utils.MAX_STARTING_BOAT_TIME_SECONDS) {
            Thread.sleep(1000);
            waitingIteration++;
        }
        linearAndAngularSpeed = autonomicController.clearAfterRotating();
        connection.sendEnginesPowerInAutonomicMode(linearAndAngularSpeed);
        Thread.sleep(1000);
    }

}
