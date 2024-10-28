package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PositionAlgorithm {
    ONLY_GPS("kurs i pozycja z GPS"),
    GPS_AND_SENSOR("kurs z sensora i pozycja z GPS"),
    BASIC_ALGORITHM("podstawowy algorytm"),
    KALMAN_FILTER("algorytm z filtrem Kalmana");

    @Getter
    private String description;
}
