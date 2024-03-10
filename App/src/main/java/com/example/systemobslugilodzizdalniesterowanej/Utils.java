package com.example.systemobslugilodzizdalniesterowanej;

import java.io.*;
import java.util.List;

public class Utils {

    public static void saveGpsToCsv(List<String[]> gpsData) throws IOException {
        FileWriter csvOutputFile = new FileWriter("/home/kacperbielak/Desktop/gps.csv", true);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            gpsData.stream()
                    .map(Utils::convertToCSV)
                    .forEach(pw::println);
        }
    }

    public static void saveCourseToCsv(List<String[]> courseData) throws IOException {
        FileWriter csvOutputFile = new FileWriter("/home/kacperbielak/Desktop/course.csv", true);
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
