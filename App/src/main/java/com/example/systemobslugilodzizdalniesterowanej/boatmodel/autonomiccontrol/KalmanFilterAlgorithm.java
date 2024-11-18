package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Setter
@NoArgsConstructor
@Slf4j
public class KalmanFilterAlgorithm {

    private static double GPS_COURSE_MAX_ACCURACY_DIFF = 30;
    private KalmanFilter kalmanFilter;

    private boolean foundGpsCourse = false;
    private Double gpsCourse = null;
    private Double sensorCourse = null;
    private Coordinate gpsLocalization = null;
    private double accelerationX = 0.0;
    private double accelerationY = 0.0;
    private double angularSpeed = 0.0;
    private Double oldGpsCourse = -1.0;
    private Double oldSensorCourse = -1.0;
    private Coordinate oldGpsLocalization = null;
    private double oldAccelerationX = -100.0;
    private double oldAccelerationY = -100.0;
    private double oldAngularSpeed = -100.0;
    private LocalDateTime now = LocalDateTime.now();

    @Getter
    private Double currentCourse = null;
    @Getter
    private Coordinate currentLocalization = null;

    @Getter
    private Lock lock = new ReentrantLock();

    public boolean designateCurrentCourseAndLocalization() throws IOException {
        if (checkValidData()) {
            if (checkNewData()) {
                double course = designateCurrentCourse();
                log.info("Starting kalman algorithm: ax - {}, ay - {}, w = {}, lat - {}, long - {}, course - {} ...",
                        accelerationX, accelerationY, angularSpeed, gpsLocalization.getLatitude(), gpsLocalization.getLongitude(), course);
                ArrayRealVector controlVector = new ArrayRealVector(new double[]{
                        accelerationX, accelerationY
                });
                kalmanFilter.predict(controlVector);
                ArrayRealVector measurementVector = new ArrayRealVector(new double[]{
                        gpsLocalization.getLongitude(), // x
                        gpsLocalization.getLatitude(),  // y
                        course,        // azymut
                        angularSpeed // predkosc katowa
                });
                kalmanFilter.correct(measurementVector);
                showCovarianceMatrix(kalmanFilter.getErrorCovarianceMatrix());
                double[] estimatedCourse = kalmanFilter.getStateEstimation();
                this.currentCourse = estimatedCourse[4];
                this.currentLocalization = new Coordinate(estimatedCourse[1], estimatedCourse[0]);
                setOldValue();

                saveDesignatedValueToCSVFile(course);
                saveCovarianceMatrixToCSVFile(kalmanFilter.getErrorCovarianceMatrix());

                log.info("Ended kalman algorithm: course - {}, lat - {}, long - {}", currentCourse, currentLocalization.getLatitude(), currentLocalization.getLongitude());
                return true;
            } else {
                log.info("Starting kalman algorithm - failed because no new data.");
                return false;
            }
        } else {
            log.info("Starting kalman algorithm - failed because dont valid data.");
            return false;
        }
    }


    public void initializeKalmanFilter() throws IOException {
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"Przyspieszenie x", "Przyspieszenie y", "Predkosc katowa", "Kurs z sensora", "Kurs z GPS", "Kurs usredniony", "Kurs z kalmana", "GPS wspol.", "Kalman wspol."});
        Utils.saveToCsv(data, now.toString() + ".csv");

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
                {0.5 * dt * dt, 0},     // wpływ przyspieszenia na położenie X
                {0, 0.5 * dt * dt},     // wpływ przyspieszenia na położenie Y
                {dt, 0},                // wpływ przyspieszenia na prędkość X
                {0, dt},                // wpływ przyspieszenia na prędkość Y
                {0, 0},                 // wpływ przyspieszenia na azymut
                {0, 0}                 // wpływ przyspieszenia na predkosc katowoa
        });

        RealMatrix H = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0, 0, 0}, // x
                {0, 1, 0, 0, 0, 0}, // y
                {0, 0, 0, 0, 1, 0}, // azymut
                {0, 0, 0, 0, 0, 1}, // predkosc katowa
        });

        // NIEPEWNOSCI MODELU
        RealMatrix Q = new Array2DRowRealMatrix(new double[][]{
                {0.01, 0, 0, 0, 0, 0},
                {0, 0.01, 0, 0, 0, 0},
                {0, 0, 0.01, 0, 0, 0},
                {0, 0, 0, 0.01, 0, 0},
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0.01}
        });

        // NIEPEWNOSCI POMIAROWE
        RealMatrix R = new Array2DRowRealMatrix(new double[][]{
                {0.0001, 0, 0, 0}, // x
                {0, 0.0001, 0, 0}, // y
                {0, 0, 0.5, 0}, // azymut
                {0, 0, 0, 0.1}  // predkosc katowe
        });

        RealMatrix initialP = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0, 0, 0},
                {0, 1, 0, 0, 0, 0},
                {0, 0, 1, 0, 0, 0},
                {0, 0, 0, 1, 0, 0},
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 1}

//                {0.0000994011761326715,	0,	0.0000773837107347111,	0,	0,	0},
//                {0,	0.0000994011761326715,	0,	0.0000773837107347111,	0,	0},
//                {0.0000773837107347092,	0,	0.0256904651708843,	0,	0,	0},
//                {0,	0.0000773837107347092,	0,	0.0256904651708843,	0,	0},
//                {0,	0,	0,	0,	0.366635772248373,	0.00324969912198902},
//                {0,	0,	0,	0,	0.00324969912198901,	0.0268459638398375}

//                {0.0000994011761323349,	0,	0.0000773837106674363,	0,	0,	0},
//                {0,	0.0000994011761323349,	0,	0.0000773837106674363,	0,	0},
//                {0.0000773837106674384,	0,	0.0256904651573303,	0,	0,	0},
//                {0,	0.0000773837106674384,	0,	0.0256904651573303,	0,	0},
//                {0,	0,	0,	0,	0.366635771024806,	0.00324969354031049},
//                {0,	0,	0,	0,	0.00324969354031049,	0.0268459383772641},
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
        } else if (foundGpsCourse && (Math.abs(gpsCourse - sensorCourse) < GPS_COURSE_MAX_ACCURACY_DIFF)) {
            return (sensorCourse + gpsCourse) / 2.0;
        } else {
            return sensorCourse;
        }
    }

    private boolean checkValidData() {
        return sensorCourse != null && gpsLocalization != null;
    }

    private boolean checkNewData() {
        return (oldGpsLocalization == null && gpsLocalization != null) || !oldGpsLocalization.equals(gpsLocalization) || oldAngularSpeed != angularSpeed || oldAccelerationX != accelerationX || oldAccelerationY != accelerationY
                || oldSensorCourse != sensorCourse || oldGpsCourse != gpsCourse;
    }

    private void setOldValue() {
        oldAngularSpeed = angularSpeed;
        oldAccelerationX = accelerationX;
        oldAccelerationY = accelerationY;
        oldGpsCourse = gpsCourse;
        oldSensorCourse = sensorCourse;
        oldGpsLocalization = gpsLocalization;
    }

    private void showCovarianceMatrix(RealMatrix covarianceMatrix) {
        log.info("Covariance matrix (P): ");
        for (int i = 0; i < covarianceMatrix.getRowDimension(); i++) {
            for (int j = 0; j < covarianceMatrix.getColumnDimension(); j++) {
                System.out.print(covarianceMatrix.getEntry(i, j) + " ");
            }
            System.out.println();
        }
    }

    private void saveCovarianceMatrixToCSVFile(RealMatrix covarianceMatrix) throws IOException {
        log.info("Save covarianve matrix to CSV file.");
        List<String[]> covariance = new ArrayList<>();
        for (int i = 0; i < kalmanFilter.getErrorCovarianceMatrix().getRowDimension(); i++) {
            String[] temp = new String[6];
            for (int j = 0; j < kalmanFilter.getErrorCovarianceMatrix().getColumnDimension(); j++) {
                temp[j] = String.valueOf(kalmanFilter.getErrorCovarianceMatrix().getEntry(i, j));
            }
            covariance.add(temp);
        }
        String[] empty = {" ", " ", " ", " ", " ", " "};
        covariance.add(empty);
        Utils.saveToCsv(covariance, now.toString() + "-covariance_matrix.csv");

    }

    private void saveDesignatedValueToCSVFile(double course) throws IOException {
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{String.valueOf(accelerationX), String.valueOf(accelerationY), String.valueOf(angularSpeed), String.valueOf(sensorCourse),
                String.valueOf(gpsCourse), String.valueOf(course), String.valueOf(this.currentCourse),
                String.valueOf(gpsLocalization.getLongitude() + ";" + gpsLocalization.getLatitude()),
                String.valueOf(this.currentLocalization.getLongitude() + ";" + this.currentLocalization.getLatitude())});
        Utils.saveToCsv(data, now.toString() + ".csv");
    }

}
