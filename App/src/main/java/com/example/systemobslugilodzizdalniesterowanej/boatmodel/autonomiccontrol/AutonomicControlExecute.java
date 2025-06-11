package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.example.systemobslugilodzizdalniesterowanej.communication.Connection;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import com.example.systemobslugilodzizdalniesterowanej.maps.OwnCoordinate;
import javafx.application.Platform;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AutonomicControlExecute {

    private final static int KALMAN_JOB_EXECUTE_SCHEDULER_MILLISECONDS = 300;
    private final static int AUTONOMIC_CONTROL_JOB_EXECUTE_SCHEDULER_MILLISECONDS = 700;
    ScheduledExecutorService autonomicScheduler = Executors.newScheduledThreadPool(1);
    ScheduledExecutorService kalmanScheduler = Executors.newScheduledThreadPool(1);
    BoatModeController boatModeController;
    Connection connection;
    KalmanFilterAlgorithm kalmanFilterAlgorithm;
    OSMMap osmMap;
    private final Label designatedCourse;
    PositionAlgorithm positionAlgorithm;
    private String testingCsvFileName;
    private LocalDateTime now = LocalDateTime.now();

    public AutonomicControlExecute(BoatModeController boatModeController, Connection connection, KalmanFilterAlgorithm kalmanFilterAlgorithm, OSMMap osmMap, Label designatedCourse, PositionAlgorithm positionAlgorithm) {
        this.boatModeController = boatModeController;
        this.connection = connection;
        this.kalmanFilterAlgorithm = kalmanFilterAlgorithm;
        this.osmMap = osmMap;
        this.designatedCourse = designatedCourse;
        this.positionAlgorithm = positionAlgorithm;

        testingCsvFileName = "last-testing-" + now.format(Utils.formatter);
        Utils.saveInitDesignatedValueToCSVFileWhileTesting(testingCsvFileName);
    }

    public void start() {
        Runnable kalmanTask = () -> {
            if (!connection.inStoppingBoatProcess()) {
                BoatMode currentBoatMode = boatModeController.getBoatMode();
                kalmanFilterAlgorithm.kalmanLocked();
                boolean correctResult = false;
                correctResult = kalmanFilterAlgorithm.designateCurrentCourseAndLocalization();
                if (correctResult) {
                    Platform.runLater(() -> {
                        designatedCourse.setText(String.format("%.2f", kalmanFilterAlgorithm.getCurrentCourse()));
                    });
                    osmMap.setCurrentCourse(kalmanFilterAlgorithm.getCurrentCourse());
                    if (currentBoatMode != BoatMode.AUTONOMIC_STARTING && currentBoatMode != BoatMode.AUTONOMIC_RUNNING) {
                        osmMap.generateTraceFromBoatPosition(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                    } else if (currentBoatMode != BoatMode.AUTONOMIC_STARTING) {
                        osmMap.setCurrentBoatPositionWhileRunning(kalmanFilterAlgorithm.getCurrentLocalization());
                    }
                }
                kalmanFilterAlgorithm.kalmanUnlocked();
            }
        };

        Runnable autonomicControlTask = () -> {
            BoatMode currentBoatMode = boatModeController.getBoatMode();
            if (currentBoatMode == BoatMode.AUTONOMIC_RUNNING) {
                connection.sendingValuesLock();
                try {
                    connection.designateAndSendEnginesPowerByAutonomicController();
                    saveDataToCSVFileWhileTesting();
                } catch (Exception ex) {
                    log.error("Error while autonomic control task: {}", ex.getMessage());
                } finally {
                    connection.sendingValuesUnlock();
                }
            }
        };

        if (positionAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
            kalmanScheduler.scheduleAtFixedRate(kalmanTask, 2000, KALMAN_JOB_EXECUTE_SCHEDULER_MILLISECONDS, TimeUnit.MILLISECONDS);
        }
        autonomicScheduler.scheduleAtFixedRate(autonomicControlTask, 5000, AUTONOMIC_CONTROL_JOB_EXECUTE_SCHEDULER_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    private void saveDataToCSVFileWhileTesting() throws IOException {
        if (positionAlgorithm == PositionAlgorithm.KALMAN_FILTER && this.kalmanFilterAlgorithm.getStartWaypoint() != null
                && this.kalmanFilterAlgorithm.getNextWaypoint() != null) {
            Double distanceToNextWaypoint = Utils.calculateDistance(osmMap.getCurrentBoatPosition(), osmMap.getNextWaypointOnTheRoad());
            Utils.saveDesignatedValueToCSVFileWhileTesting(
                    new OwnCoordinate(this.kalmanFilterAlgorithm.getStartWaypoint(), this.kalmanFilterAlgorithm.getStartWaypointToKalmanAlgorithm()),
                    new OwnCoordinate(this.kalmanFilterAlgorithm.getNextWaypoint(), this.kalmanFilterAlgorithm.getStartWaypointToKalmanAlgorithm()),
                    this.kalmanFilterAlgorithm.estimatedCoordinate,
                    this.kalmanFilterAlgorithm.getExpectedCourse().getText(),
                    String.valueOf(this.kalmanFilterAlgorithm.getCurrentCourse()),
                    String.valueOf(connection.getSensorCourse().getText()),
                    this.testingCsvFileName, true, distanceToNextWaypoint
            );
        } else if (this.osmMap.getStartWaypoint() != null && this.osmMap.getNextWaypointOnTheRoad() != null) {
            Double distanceToNextWaypoint = Utils.calculateDistance(osmMap.getCurrentBoatPosition(), osmMap.getNextWaypointOnTheRoad());
            Utils.saveDesignatedValueToCSVFileWhileTesting(
                    new OwnCoordinate(this.osmMap.getStartWaypoint(), this.osmMap.getFirstStartWaypointToCSV()),
                    new OwnCoordinate(this.osmMap.getNextWaypointOnTheRoad(), this.osmMap.getFirstStartWaypointToCSV()),
                    new OwnCoordinate(this.osmMap.getCurrentBoatPosition(), this.osmMap.getFirstStartWaypointToCSV()),
                    this.osmMap.getExpectedCourse().getText(),
                    String.valueOf(this.osmMap.getCurrentCourse()),
                    String.valueOf(connection.getSensorCourse().getText()),
                    this.testingCsvFileName, false, distanceToNextWaypoint
            );
        }
    }

}
