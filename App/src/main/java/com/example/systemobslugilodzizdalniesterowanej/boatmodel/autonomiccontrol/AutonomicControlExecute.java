package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.example.systemobslugilodzizdalniesterowanej.communication.Connection;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import com.example.systemobslugilodzizdalniesterowanej.maps.OwnCoordinate;
import com.sothawo.mapjfx.Coordinate;
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
    private final static int AUTONOMIC_CONTROL_JOB_EXECUTE_SCHEDULER_MILLISECONDS = 2000;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
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
            BoatMode currentBoatMode = boatModeController.getBoatMode();
            kalmanFilterAlgorithm.getLock().lock();
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
                    osmMap.setCurrentBoatPositionWhileRunning(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                }
            }
            kalmanFilterAlgorithm.getLock().unlock();
        };

        Runnable autonomicControlTask = () -> {
            BoatMode currentBoatMode = boatModeController.getBoatMode();
            if (currentBoatMode == BoatMode.AUTONOMIC_RUNNING) {
                try {
                    connection.getSendingValuesLock().lock();
                    connection.designateAndSendEnginesPowerByAutonomicController();
                    saveDataToCSVFileWhileTesting();
                    connection.getSendingValuesLock().unlock();
                } catch (IOException e) {
                    log.error("Error while designating and sending engines power in kalmanFilterExecuteTask: {}", e.getMessage());
                }
            }
        };

        if (positionAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
            scheduler.scheduleAtFixedRate(kalmanTask, 2000, KALMAN_JOB_EXECUTE_SCHEDULER_MILLISECONDS, TimeUnit.MILLISECONDS);
        }
        scheduler.scheduleAtFixedRate(autonomicControlTask, 5000, AUTONOMIC_CONTROL_JOB_EXECUTE_SCHEDULER_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    private void saveDataToCSVFileWhileTesting() throws IOException {
        Double expectedCourseByPoints = null;
        if (osmMap.getDesignatedWaypoints().size() >= 0 && (osmMap.getWaypointIndex()+1) < osmMap.getTestingCoordinates().size()) {
            Coordinate first = osmMap.getWaypointIndex() == 0 ? osmMap.getStartTestingCoordinate() : osmMap.getTestingCoordinates().get(osmMap.getWaypointIndex());
            Coordinate second = osmMap.getWaypointIndex() == 0 ? osmMap.getTestingCoordinates().get(osmMap.getWaypointIndex()) : osmMap.getTestingCoordinates().get(osmMap.getWaypointIndex()+1);
            expectedCourseByPoints = Utils.determineCourseBetweenTwoWaypointsForYAxis(first, second);
        }
        if (positionAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
            Utils.saveDesignatedValueToCSVFileWhileTesting(
                    new OwnCoordinate(this.kalmanFilterAlgorithm.getStartWaypoint(), this.kalmanFilterAlgorithm.getStartWaypointToKalmanAlgorithm()),
                    new OwnCoordinate(this.kalmanFilterAlgorithm.getNextWaypoint(), this.kalmanFilterAlgorithm.getStartWaypointToKalmanAlgorithm()),
                    this.kalmanFilterAlgorithm.estimatedCoordinate,
                    this.kalmanFilterAlgorithm.getExpectedCourse().getText(),
                    String.valueOf(this.kalmanFilterAlgorithm.getCurrentCourse()),
                    String.valueOf(connection.getSensorCourse().getText()),
                    this.testingCsvFileName, true, expectedCourseByPoints
            );
        } else if (this.osmMap.getStartWaypoint() != null && this.osmMap.getNextWaypointOnTheRoad() != null) {
            Utils.saveDesignatedValueToCSVFileWhileTesting(
                    new OwnCoordinate(this.osmMap.getStartWaypoint(), this.osmMap.getStartTestingCoordinate()),
                    new OwnCoordinate(this.osmMap.getNextWaypointOnTheRoad(), this.osmMap.getStartTestingCoordinate()),
                    new OwnCoordinate(this.osmMap.getCurrentBoatPosition(), this.osmMap.getStartTestingCoordinate()),
                    this.osmMap.getExpectedCourse().getText(),
                    String.valueOf(this.osmMap.getCurrentCourse()),
                    String.valueOf(connection.getSensorCourse().getText()),
                    this.testingCsvFileName, false, expectedCourseByPoints
            );
        }
    }

}
