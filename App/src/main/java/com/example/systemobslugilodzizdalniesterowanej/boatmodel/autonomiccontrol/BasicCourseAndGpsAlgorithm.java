package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import lombok.NoArgsConstructor;

/**
 * Podstawowy algorytm wyznaczania aktualnego kursu łodzi
 * <p>
 * Jeśli
 * - to pierwsza dana dla danego czujnika to przypisuję ją do kursu z danego czujnika,
 * - to kolejna to sprawdza czy nie jest ona bledna (nie rozni sie zbyt bardzo - MAX_ACCURACY_DIFF - od ostatniego kursu)
 * - dodatkowo dla odczytow z GPS bierzemy pod uwagę dopiero gdy dostaniemy 4 pomiar
 * <p>
 * Sam algorytm wyznacza kurs jako srednia z pomiarow z obu czujnikow oraz ostatniego pomiaru
 */
@NoArgsConstructor
public class BasicCourseAndGpsAlgorithm {

    private static double GPS_COURSE_MAX_ACCURACY_DIFF = 45;
    private static double SENSOR_COURSE_MAX_ACCURACY_DIFF = 45;
    private boolean foundGpsCourse = false;
    private Double gpsCourse = null;
    private int gpsCourseIndex = 0;
    private Double sensorCourse = null;
    private Double recentDesignatedCourse = null;

    public void setGpsCourseIfCorrectData(Double newGpsCourse) {
        if (newGpsCourse != 0 && gpsCourseIndex > 2) {
            if (gpsCourse == null || recentDesignatedCourse == null) {
                this.foundGpsCourse = true;
                this.gpsCourse = newGpsCourse;
            } else if (Math.abs(newGpsCourse - recentDesignatedCourse) < GPS_COURSE_MAX_ACCURACY_DIFF) {
                this.foundGpsCourse = true;
                this.gpsCourse = newGpsCourse;
            }
        }
        gpsCourseIndex++;
    }

    public void setSensorCourseIfCorrectData(Double newSensorCourse) {
        if (sensorCourse == null || recentDesignatedCourse == null) {
            this.sensorCourse = newSensorCourse;
        } else if (Math.abs(newSensorCourse - recentDesignatedCourse) < SENSOR_COURSE_MAX_ACCURACY_DIFF) {
            this.sensorCourse = newSensorCourse;
        }
    }

    public Double designateCurrentCourse() {
        if (recentDesignatedCourse == null) {
            if (gpsCourse == null || !foundGpsCourse) {
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
            if (gpsCourse == null || !foundGpsCourse) {
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
