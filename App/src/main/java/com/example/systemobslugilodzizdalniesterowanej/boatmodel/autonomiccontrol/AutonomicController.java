package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import com.sothawo.mapjfx.Marker;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.calculateDistance;

@Slf4j
public class AutonomicController {

    private static double DISTANCE_ACCURACY_METERS = 1.5;
    private static double MIN_DISTANCE_FOR_LINEAR_SPEED_METERS = 2.5;
    private static double MAX_DISTANCE_FOR_LINEAR_SPEED_METERS = 25.0;


    private OSMMap osmMap;

    @Setter
    @Getter
    private Double courseOnRotateStart = null;

    @Setter
    @Getter
    private boolean stopRotating = false;

    @Setter
    @Getter
    private int courseCount = 0;

    @Setter
    @Getter
    private boolean manuallyFinishSwimming = true;
    @Getter
    @Setter
    private boolean archiveLastWaypoint = false;

    public AutonomicController(OSMMap osmMap) {
        this.osmMap = osmMap;
    }

    public LinearAndAngularSpeed designateRightEnginesPowerOnStart() {
        return new LinearAndAngularSpeed(50.0, 0.0);
    }

    public LinearAndAngularSpeed designateLeftEnginesPowerOnStart() {
        return new LinearAndAngularSpeed(-50.0, 0.0);
    }

    public LinearAndAngularSpeed designateEnginesPower() {
        if (osmMap.getNextWaypointOnTheRoad() == null || osmMap.getFirstStartWaypointToCSV() == null) {
            osmMap.setNextWaypointOnTheRoad(osmMap.getDesignatedWaypoints().get(osmMap.getWaypointIndex()).getPosition());
            osmMap.setStartWaypoint(osmMap.getCurrentBoatPosition());
            osmMap.setFirstStartWaypointToCSV(osmMap.getCurrentBoatPosition());
        }
        double distance = calculateDistance(osmMap.getCurrentBoatPosition(), osmMap.getNextWaypointOnTheRoad());
        log.info("Distance to next waypoint: {}", distance);
        if (distance < DISTANCE_ACCURACY_METERS) {
            osmMap.incrementWaypointIndex();
            log.info("Another destination point has been reached. Current waypoint index: {}", osmMap.getWaypointIndex() + 1);
            List<Marker> markerList = osmMap.getDesignatedWaypoints();
            if (osmMap.getWaypointIndex() < markerList.size()) {
                osmMap.setNextWaypointOnTheRoad(markerList.get(osmMap.getWaypointIndex()).getPosition());
                osmMap.setStartWaypoint(osmMap.getCurrentBoatPosition());
                return determinateLinearAndAngularSpeed(distance);
            } else {
                Platform.runLater(()->{
                    osmMap.setStartWaypoint(null);
                    osmMap.setNextWaypointOnTheRoad(null);
                    osmMap.setFirstStartWaypointToCSV(null);
                    archiveLastWaypoint = true;
                });
                return null;
            }
        } else {
            return determinateLinearAndAngularSpeed(distance);
        }
    }

    public void incrementCourseCount() {
        this.courseCount++;
    }

    public LinearAndAngularSpeed clearAfterRotating() {
        this.courseCount = 0;
        this.stopRotating = false;
        this.courseOnRotateStart = null;
        return new LinearAndAngularSpeed(0, 0);
    }

    private LinearAndAngularSpeed determinateLinearAndAngularSpeed(double distance) {
        double newCourse = Utils.determineCourseBetweenTwoWaypoints(osmMap.getCurrentBoatPosition(), osmMap.getNextWaypointOnTheRoad());
        osmMap.setExpectedCourse(String.format("%.2f", newCourse));
        double linearSpeed = getLinearSpeed(distance);
        double angularSpeed = getAngularSpeed(newCourse, osmMap.getCurrentCourse());
        log.info("ANGULAR: " + angularSpeed);
        return new LinearAndAngularSpeed(angularSpeed, linearSpeed);
    }

    /**
     * Wyznaczanie predkosic liniowej w procentach. Jesli jest:
     * - bardzo blisko (distance <= MIN_DISTANCE_FOR_LINEAR_SPEED) to zwracamy minimalna predkosc (MIN_LINEAR_SPEED);
     * - bardzo daleko (distance >= MAX_DISTANCE_FOR_LINEAR_SPEED) to zwracamy maksymalna predkosc (100%);
     * - w innym przypadku mapujemy roznice i zwracamy w procentach.
     *
     * @param distanceBetweenCurrentAndNextPositions
     * @return
     */
    public double getLinearSpeed(double distanceBetweenCurrentAndNextPositions) {
        if (distanceBetweenCurrentAndNextPositions <= MIN_DISTANCE_FOR_LINEAR_SPEED_METERS) {
            return Utils.MIN_LINEAR_SPEED_PERCENTAGE;
        } else if (distanceBetweenCurrentAndNextPositions >= MAX_DISTANCE_FOR_LINEAR_SPEED_METERS) {
            return Utils.MAX_LINEAR_SPEED_PERCENTAGE;
        } else {
            return (distanceBetweenCurrentAndNextPositions / (MAX_DISTANCE_FOR_LINEAR_SPEED_METERS - MIN_DISTANCE_FOR_LINEAR_SPEED_METERS)) * Utils.MAX_LINEAR_SPEED_PERCENTAGE;
        }
    }

//    public double getAngularSpeed(double expectedCourse, double currentCourse) {
//        double courseDifference = Math.abs(expectedCourse - currentCourse);
//        double angularSpeed = (courseDifference / 360.0) * Utils.MAX_LINEAR_SPEED_PERCENTAGE;
//
//        if (courseDifference <= 180) {
//            if (currentCourse <= expectedCourse) {
//                return angularSpeed;
//            } else {
//                return -1 * angularSpeed;
//            }
//        } else {
//            if (currentCourse <= expectedCourse) {
//                return -1 * angularSpeed;
//            } else {
//                return angularSpeed;
//            }
//        }
//    }

    public int getAngularSpeed(double expectedCourse, double currentCourse) {
        // Różnica kąta (zakres -180 do +180)
        double courseDifference = (expectedCourse - currentCourse + 540) % 360 - 180;
        // Ustal znak: >0 prawo, <0 lewo
        int direction = courseDifference > 0 ? 1 : -1;
        double angle = Math.abs(courseDifference);
        int angularSpeed = (int) Math.round((angle / 180.0) * Utils.MAX_LINEAR_SPEED_PERCENTAGE);
        return direction * angularSpeed;
    }

}
