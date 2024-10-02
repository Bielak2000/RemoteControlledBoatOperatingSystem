package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
public class BasicCourseAndGpsAlgorithm {

    private Double gpsCourse = null;
    private Double sensorCourse = null;

    public double designateCurrentCourse() {
        // TODO: implementacja podstawowego algorytmu do wyznaczania kursu na podstawie kursu z GPS i sensora
        return 0.0;
    }

}
