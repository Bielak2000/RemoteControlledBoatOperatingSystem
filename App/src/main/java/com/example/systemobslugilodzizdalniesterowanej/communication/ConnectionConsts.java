package com.example.systemobslugilodzizdalniesterowanej.communication;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectionConsts {

    // MESSAGE TO BOAT
    public final static String FROM_APP_KEYBOARD_CONTROL_MODE_MARKING = "0";
    public final static String FROM_APP_MOVE_TO_AUTONOMIC_MODE = "1";
    public final static String FROM_APP_AUTONOMOUS_MODE_CONTROL = "2";
    public final static String FINISH_SWIMMING_BY_WAYPOINTS = "3";
    public final static String FROM_APP_INIT_CONNECTION = "4";

    // MESSAGE FROM BOAT
    public final static int FROM_BOAT_LIGHTING_MESSAGE = 0;
    public final static int FROM_BOAT_GPS_MESSAGE = 1;
    public final static int FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS = 2;

    public final static String BOAT_FINISHED_SWIMMING_INFORMATION = "Łódka dopłyneła do ostaniego waypointa, zmieniono tryb sterowania na tryb manualny. Jeśli chcesz ponownie wyznaczyć trasę wykonaj odpowiednie czynności jak poprzednio.";
    public final static String BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION = "Ręcznie przerwano pływanie łodzi po waypointach, zmieniono tryb sterowania na tryb manualny.";

    // TODO: do testow
    public final static int FROM_BOAT_GPS_COURSE_MESSAGE = 5;
    public final static int FROM_BOAT_SENSOR_COURSE_MESSAGE = 6;
    public final static int LINEAR_ACCELERATION_ANGULAR_SPEED_ASSIGN = 7;

}
