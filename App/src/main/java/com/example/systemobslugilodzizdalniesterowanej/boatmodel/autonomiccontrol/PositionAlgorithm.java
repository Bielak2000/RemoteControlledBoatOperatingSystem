package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PositionAlgorithm {
    ONLY_GPS("kurs i pozycja z GPS", 0),
    GPS_AND_SENSOR("kurs z sensora i pozycja z GPS", 1),
    BASIC_ALGORITHM("podstawowy algorytm", 2),
    KALMAN_FILTER("algorytm z filtrem Kalmana", 3);

    @Getter
    private String description;
    @Getter
    private int making;
}
