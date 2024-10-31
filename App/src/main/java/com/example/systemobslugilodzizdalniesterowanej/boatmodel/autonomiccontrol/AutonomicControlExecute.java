package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.communication.Connection;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AutonomicControlExecute {

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    BoatModeController boatModeController;
    Connection connection;
    KalmanFilterAlgorithm kalmanFilterAlgorithm;
    OSMMap osmMap;
    private Label designatedCourse;
    PositionAlgorithm positionAlgorithm;

    public AutonomicControlExecute(BoatModeController boatModeController, Connection connection, KalmanFilterAlgorithm kalmanFilterAlgorithm, OSMMap osmMap, Label designatedCourse, PositionAlgorithm positionAlgorithm) {
        this.boatModeController = boatModeController;
        this.connection = connection;
        this.kalmanFilterAlgorithm = kalmanFilterAlgorithm;
        this.osmMap = osmMap;
        this.designatedCourse = designatedCourse;
        this.positionAlgorithm = positionAlgorithm;
    }

    public void start() {
        Runnable task = () -> {
            BoatMode currentBoatMode = boatModeController.getBoatMode();
            if (positionAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                kalmanFilterAlgorithm.getLock().lock();
                boolean correctResult = kalmanFilterAlgorithm.designateCurrentCourseAndLocalization();
                if (correctResult) {
                    designatedCourse.setText(String.format("%.2f", kalmanFilterAlgorithm.getCurrentCourse()));
                    osmMap.setCurrentCourse(kalmanFilterAlgorithm.getCurrentCourse());
                    if (currentBoatMode != BoatMode.AUTONOMIC_STARTING && currentBoatMode != BoatMode.AUTONOMIC_RUNNING) {
                        osmMap.generateTraceFromBoatPosition(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                    } else {
                        osmMap.setCurrentBoatPositionWhileRunning(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                    }
                }
                kalmanFilterAlgorithm.getLock().unlock();
            }

            if (currentBoatMode == BoatMode.AUTONOMIC_RUNNING) {
                try {
                    connection.getSendingValuesLock().lock();
                    connection.designateAndSendEnginesPowerByAutonomicController();
                    connection.getSendingValuesLock().unlock();
                } catch (IOException e) {
                    log.error("Error while designating and sending engines power in kalmanFilterExecuteTask: {}", e.getMessage());
                }
            }
        };
        scheduler.scheduleAtFixedRate(task, 5, 1, TimeUnit.SECONDS);
    }

}
