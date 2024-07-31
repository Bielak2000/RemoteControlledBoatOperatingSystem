package com.example.systemobslugilodzizdalniesterowanej.common;

import com.sothawo.mapjfx.Coordinate;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class Utils {

    public static String FXML_RESOURCES_PATH = "/com/example/systemobslugilodzizdalniesterowanej/";

    public static void saveGpsToCsv(List<String[]> gpsData) throws IOException {
        FileWriter csvOutputFile = new FileWriter("/home/kacperbielak/Desktop/testy2/gps.csv", true);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            gpsData.stream()
                    .map(Utils::convertToCSV)
                    .forEach(pw::println);
        }
    }

    public static void saveCourseToCsv(List<String[]> courseData, String fileName) throws IOException {
        FileWriter csvOutputFile = new FileWriter("/home/kacperbielak/Desktop/testy2/" + fileName, true);
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
    private static final double EARTH_RADIUS = 6371.0;

    /**
     * Metoda do obliczania odległości między dwoma współrzędnymi geograficznymi w metrach
     *
     * @param c1 wspolrzedne pierwsze punktu
     * @param c1 wspolrzedne drugiego punktu
     * @return Odległość w metrach
     */
    public static double calculateDistance(Coordinate c1, Coordinate c2) {
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
        return EARTH_RADIUS * c * 1000;
    }

}
