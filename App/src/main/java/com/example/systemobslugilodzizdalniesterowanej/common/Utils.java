package com.example.systemobslugilodzizdalniesterowanej.common;

import com.example.systemobslugilodzizdalniesterowanej.maps.OwnCoordinate;
import com.sothawo.mapjfx.Coordinate;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Utils {

    public static double MAX_LINEAR_SPEED_PERCENTAGE = 80;
    public static double MIN_LINEAR_SPEED_PERCENTAGE = 30.0;

    public static String FXML_RESOURCES_PATH = "/com/example/systemobslugilodzizdalniesterowanej/";
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

    public static void saveGpsToCsv(List<String[]> gpsData) throws IOException {
        FileWriter csvOutputFile = new FileWriter("/home/kacperbielak/Desktop/testy2/gps.csv", true);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            gpsData.stream()
                    .map(Utils::convertToCSV)
                    .forEach(pw::println);
        }
    }

    public static void saveToCsv(List<String[]> courseData, String fileName) throws IOException {
        FileWriter csvOutputFile = new FileWriter("/home/kacperbielak/Desktop/kalman-tests/" + fileName, true);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            courseData.stream()
                    .map(Utils::convertToCSV)
                    .forEach(pw::println);
        }
    }

    private static String convertToCSV(String[] data) {
        return String.join(",", data);
    }

    // Stała oznaczająca promień Ziemi w kilometrach
    private static final double EARTH_RADIUS_KM = 6371.0;
    public static final double EARTH_RADIUS_M = 6371000;

    /**
     * Metoda do obliczania odległości między dwoma współrzędnymi geograficznymi w metrach
     *
     * @param c1 wspolrzedne pierwsze punktu
     * @param c1 wspolrzedne drugiego punktu
     * @return Odległość w metrach
     */
    public static double calculateDistance(Coordinate c1, Coordinate c2) {
        if (c1 != null && c2 != null) {
        // Konwersja stopni na radiany
        double lat1Rad = Math.toRadians(c1.getLatitude());
        double lon1Rad = Math.toRadians(c1.getLongitude());
        double lat2Rad = Math.toRadians(c2.getLatitude());
        double lon2Rad = Math.toRadians(c2.getLongitude());

        // Różnica szerokości i długości geograficznej w radianach
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        // Obliczenie odległości za pomocą formuły Haversine
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Obliczenie odległości
        return EARTH_RADIUS_KM * c * 1000;
        } else {
            return 0.0;
        }
    }

    // wykrzystywana do zmainu ukladu na lokalny
    public static double determineCourseBetweenTwoWaypoints(Coordinate firstCoordinate, Coordinate secondCoordinate) {
        double latitude1 = Math.toRadians(firstCoordinate.getLatitude());
        double latitude2 = Math.toRadians(secondCoordinate.getLatitude());
        double longDiff = Math.toRadians(secondCoordinate.getLongitude() - firstCoordinate.getLongitude());
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);
        return  (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    // zmienia o jedną oś w lewo, wykorzystywana do wyznaczania kursu między dwoma punktami
    public static double determineCourseBetweenTwoWaypointsForAutonomicController(Coordinate firstCoordinate, Coordinate secondCoordinate) {
        return (determineCourseBetweenTwoWaypoints(firstCoordinate, secondCoordinate) - 90 + 360) % 360;
    }

    public static void saveInitValToCsvForNotBasicAndKalmanAlgorithm(String fileName) {
        try {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Kurs", "Kurs oczekiwany", "GPS wspol.", "Punkt docelowy", "Punkt startowy"});
            Utils.saveToCsv(data, fileName + ".csv");
        } catch (IOException ex) {
            log.error("Error while initalize csv file: {}", ex);
        }
    }

    public static void saveDesignatedValueToCSVFile(String fileName, Coordinate currentLocalization, Double course, String expectedCourse, Coordinate nextWaypoint, Coordinate startWaypoint) {
        try {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{String.valueOf(course), expectedCourse,
                    String.valueOf(currentLocalization == null ? "brak" : (currentLocalization.getLongitude() + ";" + currentLocalization.getLatitude())),
                    String.valueOf(nextWaypoint == null ? "brak" : (nextWaypoint.getLongitude() + ";" + nextWaypoint.getLatitude())),
                    String.valueOf(startWaypoint == null ? "brak" : (startWaypoint.getLongitude() + ";" + startWaypoint.getLatitude()))});
            Utils.saveToCsv(data, fileName + ".csv");
        } catch (IOException ex) {
            log.error("Error while initalize csv file: {}", ex);
        }
    }

    public static void saveInitDesignatedValueToCSVFileWhileTesting(String fileName) {
        try {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Pkt. startowy", "Pkt. docelowy", "Pkt. aktualny", "Błąd [m]", "Kurs oczekiwany", "Kurs z sensora", "Kurs aktualny", "Odl. Nast. Wayp."});
            Utils.saveToCsv(data, fileName + ".csv");
        } catch (IOException ex) {
            log.error("Error while initalize csv file: {}", ex);
        }
    }

    public static void saveDesignatedValueToCSVFileWhileTesting(OwnCoordinate startPoint, OwnCoordinate destinationPoint, OwnCoordinate currentPoint,
                                                                String expectedCourse, String currentCourse, String sensorCourse, String fileName, boolean reeversed,
                                                                Double distanceToTheNextWaypoint) throws IOException {
        Double distance = distanceFromLine(startPoint, destinationPoint, currentPoint, reeversed);
        double x, y;
        if (reeversed) {
            x = currentPoint.getY();
            y = currentPoint.getX();

        } else {
            x = currentPoint.getX();
            y = currentPoint.getY();
        }
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{
                String.valueOf(startPoint == null ? "brak" : startPoint.getX() + ";" + startPoint.getY()),
                String.valueOf(destinationPoint == null ? "brak" : destinationPoint.getX() + ";" + destinationPoint.getY()),
                String.valueOf(currentPoint == null ? "brak" : x + ";" + y),
                String.valueOf(distance == null ? "brak" : String.format("%.2f", distance)),
                String.valueOf(expectedCourse == null ? "brak" : expectedCourse),
                String.valueOf(expectedCourse == null ? "brak" : sensorCourse),
                String.valueOf(currentCourse == null ? "brak" : currentCourse),
                String.valueOf(distanceToTheNextWaypoint == null ? "brak" : distanceToTheNextWaypoint)
        });
        Utils.saveToCsv(data, fileName + ".csv");
    }

    /**
     * @param startPoint
     * @param destinationPoint
     * @param currentPoint
     * @return odleglosc w metrach
     */
    private static Double distanceFromLine(OwnCoordinate startPoint, OwnCoordinate destinationPoint, OwnCoordinate currentPoint, boolean reversed) {
        if (startPoint != null && destinationPoint != null && currentPoint != null) {
            double A = destinationPoint.getY() - startPoint.getY();
            double B = startPoint.getX() - destinationPoint.getX();
            double C = destinationPoint.getX() * startPoint.getY() - startPoint.getX() * destinationPoint.getY();
            if (reversed) {
                return Math.abs(A * currentPoint.getY() + B * currentPoint.getX() + C) / Math.sqrt(A * A + B * B);
            } else {
                return Math.abs(A * currentPoint.getX() + B * currentPoint.getY() + C) / Math.sqrt(A * A + B * B);
            }

        } else return null;
    }

}
