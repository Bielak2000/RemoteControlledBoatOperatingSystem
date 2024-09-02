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

    public static int DISTANCE_ACCURACY_METERS = 2;
    private OSMMap osmMap;
    @Setter
    @Getter
    private boolean manuallyFinishSwimming = false;

    public AutonomicController(OSMMap osmMap) {
        this.osmMap = osmMap;
    }

    public LinearAndAngularSpeed designateRightEnginesPowerOnStart() {
        return new LinearAndAngularSpeed(50.0, 50.0);
    }

    public LinearAndAngularSpeed designateLeftEnginesPowerOnStart() {
        return new LinearAndAngularSpeed(-50.0, 50.0);
    }

    public LinearAndAngularSpeed designateEnginesPower() {
        if (calculateDistance(osmMap.getCurrentBoatPosition(), osmMap.getNextWaypointOnTheRoad()) < DISTANCE_ACCURACY_METERS) {
            osmMap.incrementWaypointIndex();
            List<Marker> markerList = osmMap.getDesignatedWaypoints();
            if (osmMap.getWaypointIndex() < markerList.size()) {
                osmMap.setNextWaypointOnTheRoad(markerList.get(osmMap.getWaypointIndex()).getPosition());
                return determinateLinearAndAngularSpeed();
            } else {
                return null;
            }
        } else {
            return determinateLinearAndAngularSpeed();
        }
    }

    private LinearAndAngularSpeed determinateLinearAndAngularSpeed() {
        double newCourse = Utils.determineCourseBetweenTwoWaypoints(osmMap.getCurrentBoatPosition(), osmMap.getNextWaypointOnTheRoad());
        osmMap.setExpectedCourse(String.valueOf(newCourse));
        double linearSpeed = getLinearSpeed(newCourse, osmMap.getCurrentCourse());
        double angularSpeed = getAngularSpeed(newCourse, osmMap.getCurrentCourse());
        return new LinearAndAngularSpeed(angularSpeed, linearSpeed);
    }

    // TODO: wyznaczenie predkosci liniowej
    private double getLinearSpeed(double expectedCourse, double currentCourse) {
        return 0.0;
    }

    // TODO: wyznaczenie predkosci katowej
    private double getAngularSpeed(double expectedCourse, double currentCourse) {
        return 0.0;
    }

}
