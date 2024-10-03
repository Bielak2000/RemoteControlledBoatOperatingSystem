package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import lombok.NoArgsConstructor;

/**
 * Podstawowy algorytm wyznaczania aktualnego kursu łodzi
 * <p>
 * Jeśli
 * - to pierwsza dana dla danego czujnika to przypisuję ją do kursu z danego czujnika,
 * - to kolejna to sprawdza czy nie jest ona bledna (nie rozni sie zbyt bardzo - MAX_ACCURACY_DIFF - od znanych juz kursow)
 * <p>
 * Sam algorytm wyznacza kurs jako srednia z pomiarow z obu czujnikow oraz ostatniego pomiaru
 */
@NoArgsConstructor
public class BasicCourseAndGpsAlgorithm {

    private static double MAX_ACCURACY_DIFF = 60;
    private Double gpsCourse = null;
    private Double sensorCourse = null;
    private Double recentDesignatedCourse = null;

    public void setGpsCourseIfCorrectData(Double newGpsCourse) {
        if (gpsCourse == null || recentDesignatedCourse == null) {
            this.gpsCourse = newGpsCourse;
        } else if (Math.abs(newGpsCourse - recentDesignatedCourse) < MAX_ACCURACY_DIFF) {
            this.gpsCourse = newGpsCourse;
        }
    }

    public void setSensorCourseIfCorrectData(Double newSensorCourse) {
        if (sensorCourse == null || recentDesignatedCourse == null) {
            this.sensorCourse = newSensorCourse;
        } else if (Math.abs(newSensorCourse - recentDesignatedCourse) < MAX_ACCURACY_DIFF) {
            this.sensorCourse = newSensorCourse;
        }
    }

    public double designateCurrentCourse() {
        if (recentDesignatedCourse == null) {
            if (gpsCourse == null) {
                recentDesignatedCourse = sensorCourse;
                return sensorCourse;
            } else if (sensorCourse == null) {
                recentDesignatedCourse = gpsCourse;
                return gpsCourse;
            } else {
                double newCourse = (gpsCourse + sensorCourse) / 2.0;
                recentDesignatedCourse = newCourse;
                return newCourse;
            }
        } else {
            if (gpsCourse == null) {
                double newCourse = (sensorCourse + recentDesignatedCourse) / 2.0;
                recentDesignatedCourse = newCourse;
                return newCourse;
            } else if (sensorCourse == null) {
                double newCourse = (gpsCourse + recentDesignatedCourse) / 2.0;
                recentDesignatedCourse = newCourse;
                return newCourse;
            } else {
                double newCourse = (gpsCourse + sensorCourse + recentDesignatedCourse) / 3.0;
                recentDesignatedCourse = newCourse;
                return newCourse;
            }
        }
    }

}
