package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import com.sothawo.mapjfx.Marker;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.calculateDistance;

@Slf4j
public class AutonomicController {

    private static int DISTANCE_ACCURACY_METERS = 2;
    private static double MIN_DISTANCE_FOR_LINEAR_SPEED_METERS = 1.5;
    private static double MAX_DISTANCE_FOR_LINEAR_SPEED_METERS = 20.0;
    private static double MIN_LINEAR_SPEED_PERCENTAGE = 10.0;


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
        if(osmMap.getNextWaypointOnTheRoad() == null) {
            osmMap.setNextWaypointOnTheRoad(osmMap.getDesignatedWaypoints().get(osmMap.getWaypointIndex()).getPosition());
        }
        double distance = calculateDistance(osmMap.getCurrentBoatPosition(), osmMap.getNextWaypointOnTheRoad());
        log.info("Distance to next waypoint: {}", distance);
        if (distance < DISTANCE_ACCURACY_METERS) {
            osmMap.incrementWaypointIndex();
            log.info("Another destination point has been reached. Current waypoint index: {}", osmMap.getWaypointIndex() + 1);
            List<Marker> markerList = osmMap.getDesignatedWaypoints();
            if (osmMap.getWaypointIndex() < markerList.size()) {
                osmMap.setNextWaypointOnTheRoad(markerList.get(osmMap.getWaypointIndex()).getPosition());
                return determinateLinearAndAngularSpeed(distance);
            } else {
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
    private double getLinearSpeed(double distanceBetweenCurrentAndNextPositions) {
        if (distanceBetweenCurrentAndNextPositions <= MIN_DISTANCE_FOR_LINEAR_SPEED_METERS) {
            return MIN_LINEAR_SPEED_PERCENTAGE;
        } else if (distanceBetweenCurrentAndNextPositions >= MAX_DISTANCE_FOR_LINEAR_SPEED_METERS) {
            return 80.0;
        } else {
            return (distanceBetweenCurrentAndNextPositions / (MAX_DISTANCE_FOR_LINEAR_SPEED_METERS - MIN_LINEAR_SPEED_PERCENTAGE)) * 80;
        }
    }

    private double getAngularSpeed(double expectedCourse, double currentCourse) {
        double courseDifference = expectedCourse - currentCourse;
        double angularSpeed = (courseDifference / 360.0) * 80;
        if(Math.abs(courseDifference) < 180 ) {
            angularSpeed = -1 * angularSpeed;
        }
        return angularSpeed;
    }

}
