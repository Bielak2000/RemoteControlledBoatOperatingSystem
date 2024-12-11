package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.sothawo.mapjfx.Coordinate;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
@Slf4j
public class BasicCourseAndGpsAlgorithm {

    private static double GPS_COURSE_MAX_ACCURACY_DIFF = 45;
    private static double SENSOR_COURSE_MAX_ACCURACY_DIFF = 45;
    private boolean foundGpsCourse = false;
    private Double gpsCourse = null;
    private int gpsCourseIndex = 0;
    private Double sensorCourse = null;
    private Double recentDesignatedCourse = null;
    private LocalDateTime now = LocalDateTime.now();
    private Label expectedCourse;

    public BasicCourseAndGpsAlgorithm(Label expectedCourse) {
        this.expectedCourse = expectedCourse;
    }

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

    public void saveInitValToCsv() {
        try {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Kurs GPS", "Kurs sensor", "Wyzonaczny kurs", "Kurs oczekiwany", "GPS wspol.", "Punkt docelowy", "Punkt startowy"});
            Utils.saveToCsv(data, "basic-" + now.format(Utils.formatter) + ".csv");
        } catch (IOException ex) {
            log.error("Error while initalize csv file: {}", ex);
        }
    }

    public void saveDesignatedValueToCSVFile(Coordinate currentLocalization, Double course, Coordinate nextWaypoint, Coordinate startWaypoint) {
        try {
            List<String[]> data = new ArrayList<>();
            if (course == null) {
                course = recentDesignatedCourse;
            }
            data.add(new String[]{String.valueOf(gpsCourse), String.valueOf(sensorCourse), String.valueOf(course), expectedCourse.getText(),
                    String.valueOf(currentLocalization == null ? "brak" : (currentLocalization.getLongitude() + ";" + currentLocalization.getLatitude())),
                    String.valueOf(nextWaypoint == null ? "brak" : (nextWaypoint.getLongitude() + ";" + nextWaypoint.getLatitude())),
                    String.valueOf(startWaypoint == null ? "brak" : startWaypoint.getLongitude() + ";" + startWaypoint.getLatitude())});
            Utils.saveToCsv(data, "basic-" + now.format(Utils.formatter) + ".csv");
        } catch (IOException ex) {
            log.error("Error while initalize csv file: {}", ex);
        }
    }

}
