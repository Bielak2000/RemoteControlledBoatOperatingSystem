package com.example.systemobslugilodzizdalniesterowanej.boatmodel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PositionAlgorithm {
    ONLY_GPS("Kurs i pozycja z GPS"),
    GPS_AND_SENSOR("Kurs z sensora i pozycja z GPS"),
    BASIC_ALGORITHM("Podstawowy algorytm"),
    KALMAN_FILTER("Algorytm z filtrem Kalmana");

    @Getter
    private String description;
}
