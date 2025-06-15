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
    private static double MAX_DISTANCE_FOR_LINEAR_SPEED_METERS = 15.0;


    private OSMMap osmMap;

    @Setter
    @Getter
    private Double courseOnRotateStart = null;

    @Setter
    @Getter
    private boolean stopRotating = true;

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
        return new LinearAndAngularSpeed(50.0, 0.0, Utils.ANGULAR_FACTORY_NORMAL);
    }

    public LinearAndAngularSpeed designateLeftEnginesPowerOnStart() {
        return new LinearAndAngularSpeed(-60.0, 0.0, Utils.ANGULAR_FACTORY_NORMAL);
    }

    public LinearAndAngularSpeed designateSpeeds() {
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
                Platform.runLater(() -> {
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

    public double designateAngularFactory(double courseDifference) {
        if (courseDifference > Utils.COURSE_DIFFERENCE_FOR_MAX_ANGULAR_FACTORY) {
            return Utils.ANGULAR_FACTORY_MAX;
        } else return Utils.ANGULAR_FACTORY_NORMAL;
    }

    public void incrementCourseCount() {
        this.courseCount++;
    }

    public LinearAndAngularSpeed clearAfterRotating() {
        this.courseCount = 0;
        this.courseOnRotateStart = null;
        return new LinearAndAngularSpeed(0, 0, Utils.ANGULAR_FACTORY_NORMAL);
    }

    private LinearAndAngularSpeed determinateLinearAndAngularSpeed(double distance) {
        double newCourse = (Utils.determineCourseBetweenTwoWaypoints(osmMap.getCurrentBoatPosition(), osmMap.getNextWaypointOnTheRoad()) + 270) % 360;// + 270) % 360;
        osmMap.setExpectedCourse(String.format("%.2f", newCourse));
        double linearSpeed = getLinearSpeed(distance);
        double angularSpeed = getAngularSpeed(newCourse, osmMap.getCurrentCourse());
        log.info("ANGULAR: " + angularSpeed);
        double differenceCourse = Math.abs(newCourse - osmMap.getCurrentCourse());
        double minDifference = Math.min(differenceCourse, 360.0 - differenceCourse);
        double angularFactory = designateAngularFactory(Math.abs(minDifference));
        return new LinearAndAngularSpeed(angularSpeed, linearSpeed, angularFactory);
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

    public int getAngularSpeed(double expectedCourse, double currentCourse) {
        double courseDifference = (expectedCourse - currentCourse + 540) % 360 - 180;
        int direction = courseDifference > 0 ? 1 : -1;
        double angle = Math.abs(courseDifference);
        int angularSpeed = (int) Math.round((angle / 180.0) * Utils.MAX_LINEAR_SPEED_PERCENTAGE);
        return direction * angularSpeed;
    }

    public int getAngularSpeedDirection(double expectedCourse, double currentCourse) {
        double courseDifference = (expectedCourse - currentCourse + 540) % 360 - 180;
        return courseDifference > 0 ? 1 : -1;
    }

}
