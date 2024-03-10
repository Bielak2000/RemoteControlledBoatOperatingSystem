package com.example.systemobslugilodzizdalniesterowanej;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class Utils {

    public static void saveGpsToCsv(List<String[]> gpsData) throws FileNotFoundException {
        File csvOutputFile = new File("/home/kacperbielak/Desktop/gps.csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            gpsData.stream()
                    .map(Utils::convertToCSV)
                    .forEach(pw::println);
        }
    }

    public static void saveCourseToCsv(List<String[]> courseData) throws FileNotFoundException {
        File csvOutputFile = new File("/home/kacperbielak/Desktop/course.csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            courseData.stream()
                    .map(Utils::convertToCSV)
                    .forEach(pw::println);
        }
    }

    private static String convertToCSV(String[] data) {
        return String.join(",", data);
    }

}
