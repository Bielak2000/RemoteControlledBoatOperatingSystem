package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.example.systemobslugilodzizdalniesterowanej.maps.OwnCoordinate;
import com.sothawo.mapjfx.Coordinate;
import javafx.scene.control.Label;
import lombok.Getter;
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
import java.util.stream.Collectors;

@Setter
@Slf4j
public class KalmanFilterAlgorithm {

    private static double GPS_COURSE_MAX_ACCURACY_DIFF = 30;
    private static int MIN_GPS_CALIBRATION_COUNT = 5;
    private static int GPS_CALIBRATION_ACCURACY = 50;

    private KalmanFilter kalmanFilter;
    private Label expectedCourse;

    private boolean foundGpsCourse = false;
    private Double gpsCourse = null;
    private Double sensorCourse = null;

    private List<Coordinate> gpsLocalizationCalibration = new ArrayList<>();
    private OwnCoordinate gpsLocalization = null;
    private Coordinate startWaypointToKalmanAlgorithm = null;

    @Getter
    private Coordinate nextWaypoint = null;
    private Coordinate startWaypoint = null;
    private double accelerationX = 0.0;
    private double accelerationY = 0.0;
    private double angularSpeed = 0.0; // rad/s
    private Double oldGpsCourse = -1.0;
    private Double oldSensorCourse = -1.0;
    private OwnCoordinate oldGpsLocalization = null;
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

    public KalmanFilterAlgorithm(Label expectedCourse) {
        this.expectedCourse = expectedCourse;
    }

    public boolean designateCurrentCourseAndLocalization() {
        if (checkValidData()) {
            double course = designateCurrentCourse();
            log.info("Starting kalman algorithm: ax - {}, ay - {}, w = {}, x - {}, y - {}, course - {} ...",
                    accelerationX, accelerationY, angularSpeed, gpsLocalization.getX(), gpsLocalization.getY(), course);
            kalmanFilter.predict();
            ArrayRealVector measurementVector = new ArrayRealVector(new double[]{
                    gpsLocalization.getX(),      // x
                    gpsLocalization.getY(),      // y
                    accelerationX,               // ax
                    accelerationY,               // ay
                    course,                      // azymut
                    angularSpeed * 57.2958       // predkosc katowa
            });
            kalmanFilter.correct(measurementVector);
            showCovarianceMatrix(kalmanFilter.getErrorCovarianceMatrix());
            double[] estimatedData = kalmanFilter.getStateEstimation();
            this.currentCourse = estimatedData[6];
            OwnCoordinate estimatedCoordinate = new OwnCoordinate(estimatedData[1], estimatedData[0]);
            this.currentLocalization = estimatedCoordinate.transformCoordinateToGlobalCoordinateSystem(startWaypointToKalmanAlgorithm);
            setOldValue();
            try {
                saveDesignatedValueToCSVFile(course);
                saveCovarianceMatrixToCSVFile(kalmanFilter.getErrorCovarianceMatrix());
            } catch (IOException ex) {
                log.error("Error while initialize csv files: {}", ex);
            }
            log.info("Ended kalman algorithm: course - {}, x - {}, y - {}", currentCourse, estimatedCoordinate.getX(), estimatedCoordinate.getY());
            return true;
        } else {
            log.info("Starting kalman algorithm - failed because dont valid data.");
            return false;
        }
    }


    public void initializeKalmanFilter() {
        try {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Przyspieszenie x", "Przyspieszenie y", "Predkosc katowa", "Kurs z sensora", "Kurs z GPS", "Kurs usredniony", "Kurs z kalmana", "Kurs oczekiwany", "GPS wspol. (xy)", "Kalman wspol.", "Punkt docelowy", "Punkt startowy"});
            Utils.saveToCsv(data, "kalman-" + now.format(Utils.formatter) + ".csv");
        } catch (IOException ex) {
            log.error("Error while initialize csv files: {}", ex);
        }


        log.info("Starting kalman filter initialization ...");
        double dt = 0.3;

        RealMatrix A = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0, 0, 0, 0, 0}, // x
                {0, 1, 0, 0, 0, 0, 0, 0}, // y
                {dt, 0, 1, 0, 0, 0, 0, 0},  // Vx
                {0, dt, 0, 1, 0, 0, 0, 0},  // Vy
                {0.5 * dt * dt, 0, dt, 0, 1, 0, 0, 0},  // ax
                {0, 0.5 * dt * dt, 0, dt, 0, 1, 0, 0}, // ay
                {0, 0, 0, 0, 0, 0, 1, 0}, // azymut
                {0, 0, 0, 0, 0, 0, dt, 1}   // predkosc katowa
        });

        RealMatrix B = null;

        RealMatrix H = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0, 0, 0, 0, 0}, // x
                {0, 1, 0, 0, 0, 0, 0, 0}, // y
                {0, 0, 0, 0, 1, 0, 0, 0}, // ax
                {0, 0, 0, 0, 0, 1, 0, 0}, // ay
                {0, 0, 0, 0, 0, 0, 1, 0}, // ax
                {0, 0, 0, 0, 0, 0, 0, 1} // ay
        });

        // NAJLEPSZE OPCJE - nr 4
        RealMatrix Q4 = new Array2DRowRealMatrix(new double[][]{
                {0.01, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.01, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.05, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.05, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.05, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.05, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.5}
        });

        RealMatrix R4 = new Array2DRowRealMatrix(new double[][]{
                {0.1, 0, 0, 0, 0, 0}, // x
                {0, 0.1, 0, 0, 0, 0}, // y
                {0, 0, 0.1, 0, 0, 0}, // ax
                {0, 0, 0, 0.1, 0, 0}, // ay
                {0, 0, 0, 0, 0.5, 0}, // azymut
                {0, 0, 0, 0, 0, 0.1}  // predkosc katowe
        });

        RealMatrix Q6 = new Array2DRowRealMatrix(new double[][]{
                {0.0001, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.0001, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.005, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.005, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.005, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.005, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.5}
        });

        RealMatrix R6 = new Array2DRowRealMatrix(new double[][]{
                {0.001, 0, 0, 0, 0, 0}, // x
                {0, 0.001, 0, 0, 0, 0}, // y
                {0, 0, 0.01, 0, 0, 0}, // ax
                {0, 0, 0, 0.01, 0, 0}, // ay
                {0, 0, 0, 0, 0.5, 0}, // azymut
                {0, 0, 0, 0, 0, 0.1}  // predkosc katowe
        });

        RealMatrix Q7 = new Array2DRowRealMatrix(new double[][]{
                {0.002, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.002, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.05, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.05, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.05, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.05, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.1}
        });

        RealMatrix R7 = new Array2DRowRealMatrix(new double[][]{
                {0.02, 0, 0, 0, 0, 0}, // x
                {0, 0.02, 0, 0, 0, 0}, // y
                {0, 0, 0.1, 0, 0, 0}, // ax
                {0, 0, 0, 0.1, 0, 0}, // ay
                {0, 0, 0, 0, 0.1, 0}, // azymut
                {0, 0, 0, 0, 0, 0.01}  // predkosc katowe
        });

        RealMatrix Q8 = new Array2DRowRealMatrix(new double[][]{
                {0.002, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.002, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.05, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.05, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.05, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.05, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.5}
        });

        RealMatrix R8 = new Array2DRowRealMatrix(new double[][]{
                {0.02, 0, 0, 0, 0, 0}, // x
                {0, 0.02, 0, 0, 0, 0}, // y
                {0, 0, 0.1, 0, 0, 0}, // ax
                {0, 0, 0, 0.1, 0, 0}, // ay
                {0, 0, 0, 0, 0.5, 0}, // azymut
                {0, 0, 0, 0, 0, 0.1}  // predkosc katowe
        });

        RealMatrix Q9 = new Array2DRowRealMatrix(new double[][]{
                {0.1, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.1, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.05, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.05, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.05, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.05, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.5}
        });

        RealMatrix R9 = new Array2DRowRealMatrix(new double[][]{
                {0.5, 0, 0, 0, 0, 0}, // x
                {0, 0.5, 0, 0, 0, 0}, // y
                {0, 0, 0.1, 0, 0, 0}, // ax
                {0, 0, 0, 0.1, 0, 0}, // ay
                {0, 0, 0, 0, 0.5, 0}, // azymut
                {0, 0, 0, 0, 0, 0.1}  // predkosc katowe
        });

        RealMatrix Q10 = new Array2DRowRealMatrix(new double[][]{
                {0.01, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.01, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.05, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.05, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.05, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.05, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.5}
        });

        RealMatrix R10 = new Array2DRowRealMatrix(new double[][]{
                {0.1, 0, 0, 0, 0, 0}, // x
                {0, 0.1, 0, 0, 0, 0}, // y
                {0, 0, 0.1, 0, 0, 0}, // ax
                {0, 0, 0, 0.1, 0, 0}, // ay
                {0, 0, 0, 0, 0.5, 0}, // azymut
                {0, 0, 0, 0, 0, 0.1}  // predkosc katowe
        });

        RealMatrix Q11 = new Array2DRowRealMatrix(new double[][]{
                {0.001, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.001, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.05, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.05, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.05, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.05, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.5}
        });

        RealMatrix R11 = new Array2DRowRealMatrix(new double[][]{
                {0.01, 0, 0, 0, 0, 0}, // x
                {0, 0.01, 0, 0, 0, 0}, // y
                {0, 0, 0.1, 0, 0, 0}, // ax
                {0, 0, 0, 0.1, 0, 0}, // ay
                {0, 0, 0, 0, 0.5, 0}, // azymut
                {0, 0, 0, 0, 0, 0.1}  // predkosc katowe
        });

        RealMatrix Q12 = new Array2DRowRealMatrix(new double[][]{
                {0.01, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.01, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.01, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.01, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.01, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.01, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.5}
        });

        RealMatrix R13 = new Array2DRowRealMatrix(new double[][]{
                {0.1, 0, 0, 0, 0, 0}, // x
                {0, 0.1, 0, 0, 0, 0}, // y
                {0, 0, 0.01, 0, 0, 0}, // ax
                {0, 0, 0, 0.01, 0, 0}, // ay
                {0, 0, 0, 0, 0.5, 0}, // azymut
                {0, 0, 0, 0, 0, 0.1}  // predkosc katowe
        });

        RealMatrix Q14 = new Array2DRowRealMatrix(new double[][]{
                {0.001, 0, 0, 0, 0, 0, 0, 0},
                {0, 0.001, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.01, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.01, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.01, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.01, 0, 0},
                {0, 0, 0, 0, 0, 0, 2, 0},
                {0, 0, 0, 0, 0, 0, 0, 1}
        });

        RealMatrix R14 = new Array2DRowRealMatrix(new double[][]{
                {0.01, 0, 0, 0, 0, 0}, // x
                {0, 0.01, 0, 0, 0, 0}, // y
                {0, 0, 0.1, 0, 0, 0}, // ax
                {0, 0, 0, 0.1, 0, 0}, // ay
                {0, 0, 0, 0, 1, 0}, // azymut
                {0, 0, 0, 0, 0, 0.5}  // predkosc katowe
        });

        RealMatrix initialP = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0, 0, 0, 0, 0},
                {0, 1, 0, 0, 0, 0, 0, 0},
                {0, 0, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 1, 0, 0, 0, 0},
                {0, 0, 0, 0, 1, 0, 0, 0},
                {0, 0, 0, 0, 0, 1, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 0, 1}
        });

        // od Q1, Q2, Q4, ..., Q14
        ArrayRealVector initialState = new ArrayRealVector(new double[]{0, 0, 0, 0, 0, 0, 0, 0});
        DefaultProcessModel processModel = new DefaultProcessModel(A, B, Q4, initialState, initialP);
        DefaultMeasurementModel measurementModel = new DefaultMeasurementModel(H, R4);
        kalmanFilter = new KalmanFilter(processModel, measurementModel);
        log.info("Ended kalman filter initialization");
    }

    public void setGpsLocalizationWithCalibrationHandler(Coordinate newLocalization) {
        if (gpsLocalizationCalibration.size() < MIN_GPS_CALIBRATION_COUNT) {
            gpsLocalizationCalibration.add(newLocalization);
        } else if (gpsLocalization == null && startWaypointToKalmanAlgorithm == null) {
            gpsLocalizationCalibration.add(newLocalization);
            List<Coordinate> closePoints = gpsLocalizationCalibration.stream()
                    .filter(pointA -> gpsLocalizationCalibration.stream()
                            .allMatch(pointB -> Utils.calculateDistance(pointA, pointB) <= GPS_CALIBRATION_ACCURACY)
                    )
                    .collect(Collectors.toList());
            startWaypointToKalmanAlgorithm = closePoints.get(closePoints.size() - 1);
            gpsLocalization = new OwnCoordinate(closePoints.get(closePoints.size() - 1), startWaypointToKalmanAlgorithm);
        } else {
            gpsLocalization = new OwnCoordinate(newLocalization, startWaypointToKalmanAlgorithm);
        }
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
            String[] temp = new String[8];
            for (int j = 0; j < kalmanFilter.getErrorCovarianceMatrix().getColumnDimension(); j++) {
                temp[j] = String.valueOf(kalmanFilter.getErrorCovarianceMatrix().getEntry(i, j));
            }
            covariance.add(temp);
        }
        String[] empty = {" ", " ", " ", " ", " ", " "};
        covariance.add(empty);
        Utils.saveToCsv(covariance, "kalman-" + now.format(Utils.formatter) + "-covariance_matrix.csv");

    }

    private void saveDesignatedValueToCSVFile(double course) throws IOException {
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{String.valueOf(accelerationX), String.valueOf(accelerationY), String.valueOf(angularSpeed), String.valueOf(sensorCourse),
                String.valueOf(gpsCourse), String.valueOf(course), String.valueOf(this.currentCourse),
                expectedCourse.getText(),
                String.valueOf(gpsLocalization.getX() + ";" + gpsLocalization.getY()),
                String.valueOf(this.currentLocalization.getLongitude() + ";" + this.currentLocalization.getLatitude()),
                String.valueOf(this.nextWaypoint == null ? "brak" : this.nextWaypoint.getLongitude() + ";" + this.nextWaypoint.getLatitude()),
                String.valueOf(this.startWaypoint == null ? "brak" : this.startWaypoint.getLongitude() + ";" + this.startWaypoint.getLatitude())});
        Utils.saveToCsv(data, "kalman-" + now.format(Utils.formatter) + ".csv");
    }

}
