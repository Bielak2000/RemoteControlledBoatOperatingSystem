package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.sothawo.mapjfx.Coordinate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
public class KalmanFilterAlgorithm {

    private Double gpsCourse = null;
    private Double sensorCourse = null;
    private Coordinate localization = null;
    @Getter
    private Double currentCourse = null;
    @Getter
    private Coordinate currentLocalization = null;

    public void designateCurrentCourseAndLocalization() {
        // TODO: implementacja algorytmu z filtrem kalmana do wyznaczania kursu i lokalizacji na podstawie kursu z GPS i sensora oraz lokalizacji
        this.currentCourse = 0.0;
        this.currentLocalization = new Coordinate(0.0, 0.0);
    }

}
