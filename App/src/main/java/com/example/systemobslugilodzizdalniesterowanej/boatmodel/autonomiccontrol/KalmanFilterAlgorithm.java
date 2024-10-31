package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.sothawo.mapjfx.Coordinate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Setter
@NoArgsConstructor
@Slf4j
public class KalmanFilterAlgorithm {

    private static double GPS_COURSE_MAX_ACCURACY_DIFF = 30;
    private KalmanFilter kalmanFilter;

    private Double gpsCourse = null;
    private Double sensorCourse = null;
    private Coordinate gpsLocalization = null;
    private double accelerationX = 0.0;
    private double accelerationY = 0.0;
    private double accelerationA = 0.0;
    private Double oldGpsCourse = -1.0;
    private Double oldSensorCourse = -1.0;
    private Coordinate oldGpsLocalization = null;
    private double oldAccelerationX = -1.0;
    private double oldAccelerationY = -1.0;
    private double oldAccelerationA = -1.0;


    @Getter
    private Double currentCourse = null;
    @Getter
    private Coordinate currentLocalization = null;

    @Getter
    private Lock lock = new ReentrantLock();

    public void designateCurrentCourseAndLocalization() {
        if (checkValidData()) {
            if (checkNewData()) {
                log.info("Starting kalman algorithm ...");
                ArrayRealVector controlVector = new ArrayRealVector(new double[]{
                        accelerationX, accelerationY, accelerationA
                });
                kalmanFilter.predict(controlVector);
                ArrayRealVector measurementVector = new ArrayRealVector(new double[]{
                        gpsLocalization.getLongitude(), // x
                        gpsLocalization.getLatitude(),  // y
                        designateCurrentCourse()        // azymut
                });
                kalmanFilter.correct(measurementVector);
                double[] estimatedCourse = kalmanFilter.getStateEstimation();
                this.currentCourse = estimatedCourse[4];
                this.currentLocalization = new Coordinate(estimatedCourse[1], estimatedCourse[0]);
                setOldValue();
                log.info("Ended kalman algorithm: course - {}, lat - {}, long - {}", currentCourse, currentLocalization.getLatitude(), currentLocalization.getLongitude());
            } else {
                log.info("Starting kalman algorithm - failed because no new data.");
            }
        } else {
            log.info("Starting kalman algorithm - failed because dont valid data.");
        }
    }


    public void initializeKalmanFilter() {
        log.info("Starting kalman filter initialization ...");
        double dt = 0.5;

        RealMatrix A = new Array2DRowRealMatrix(new double[][]{
                {1, 0, dt, 0, 0, 0}, // x
                {0, 1, 0, dt, 0, 0}, // y
                {0, 0, 1, 0, 0, 0},  // Vx
                {0, 0, 0, 1, 0, 0},  // Vy
                {0, 0, 0, 0, 1, dt}, // azymut
                {0, 0, 0, 0, 0, 1}   // predkosc katowa
        });

        RealMatrix B = new Array2DRowRealMatrix(new double[][]{
                {0.5 * dt * dt, 0, 0},     // wpływ przyspieszenia na położenie X
                {0, 0.5 * dt * dt, 0},     // wpływ przyspieszenia na położenie Y
                {dt, 0, 0},                // wpływ przyspieszenia na prędkość X
                {0, dt, 0},                // wpływ przyspieszenia na prędkość Y
                {0, 0, 0.5 * dt * dt},     // wpływ przyspieszenia kątowego na azymut
                {0, 0, dt}                 // wpływ
        });

        RealMatrix H = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0, 0, 0}, // x
                {0, 1, 0, 0, 0, 0}, // y
                {0, 0, 0, 0, 0, 0}, // Vx
                {0, 0, 0, 0, 0, 0}, // Vy
                {0, 0, 0, 0, 1, 0}, // azymut
                {0, 0, 0, 0, 0, 0}, // predkosc katowa
        });

        RealMatrix Q = new Array2DRowRealMatrix(new double[][]{
                {0.01, 0, 0, 0, 0, 0},
                {0, 0.01, 0, 0, 0, 0},
                {0, 0, 0.01, 0, 0, 0},
                {0, 0, 0, 0.01, 0, 0},
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0.01, 0, 0}
        });

        RealMatrix R = new Array2DRowRealMatrix(new double[][]{
                {0.1, 0, 0, 0}, // ax
                {0, 0.1, 0, 0}, // ay
                {0, 0, 1, 0},   // azymut
                {0, 0, 0, 0.1}  // przyspieszenie katowe
        });

        RealMatrix initialP = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0, 0, 0},
                {0, 1, 0, 0, 0, 0},
                {0, 0, 1, 0, 0, 0},
                {0, 0, 0, 1, 0, 0},
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 1}
        });

        ArrayRealVector initialState = new ArrayRealVector(new double[]{0, 0, 0, 0, 0, 0});
        DefaultProcessModel processModel = new DefaultProcessModel(A, B, Q, initialState, initialP);
        DefaultMeasurementModel measurementModel = new DefaultMeasurementModel(H, R);
        kalmanFilter = new KalmanFilter(processModel, measurementModel);
        log.info("Ended kalman filter initialization");
    }

    private double designateCurrentCourse() {
        if (gpsCourse == null) {
            return sensorCourse;
        } else if ((Math.abs(gpsCourse - sensorCourse) < GPS_COURSE_MAX_ACCURACY_DIFF)) {
            return (sensorCourse + gpsCourse) / 2.0;
        } else {
            return sensorCourse;
        }
    }

    private boolean checkValidData() {
        return sensorCourse != null && gpsLocalization != null;
    }

    private boolean checkNewData() {
        return !oldGpsLocalization.equals(gpsLocalization) || oldAccelerationA != accelerationA || oldAccelerationX != accelerationX || oldAccelerationY != accelerationY
                || oldSensorCourse != sensorCourse || gpsCourse != gpsCourse || gpsCourse == null || sensorCourse == null || gpsLocalization == null
                || (oldAccelerationA == 0 && accelerationA == -1) || (oldAccelerationX == 0 && accelerationX == -1) || (oldAccelerationY == 0 && accelerationY == -1);
    }

    private void setOldValue() {
        oldAccelerationA = accelerationA;
        oldAccelerationX = accelerationX;
        oldAccelerationY = accelerationY;
        oldGpsCourse = gpsCourse;
        oldSensorCourse = sensorCourse;
        oldGpsLocalization = gpsLocalization;
    }

}
