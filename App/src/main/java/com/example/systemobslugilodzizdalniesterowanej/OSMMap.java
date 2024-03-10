package com.example.systemobslugilodzizdalniesterowanej;

import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OSMMap {
    private static String WMSUrl = "https://mapy.geoportal.gov.pl/wss/service/PZGIK/ORTO/WMS/HighResolution";
    private MapView mapView;
    private List<Marker> markerList = new ArrayList<>();
    private CoordinateLine coordinateLine = null;
    private List<CoordinateLine> coordinateLines = new ArrayList<>();
    private BoatModeController boatModeController;
    private Boolean foundBoatPosition;
    private Marker currentBoatPositionWhileRunning = null;
    Label expectedCourse;

    /**
     * GREEN TAG - waypoint determined by user
     * RED TAG - waypoint determined by boat duringo autonomic swimming
     * BLUE TAG - first boat localization
     */
    public OSMMap(MapView mapView, BoatModeController boatModeController, Label expectedCourse) {
        this.mapView = mapView;
        this.boatModeController = boatModeController;
        this.foundBoatPosition = false;
        this.expectedCourse = expectedCourse;
        mapInitialize();
    }

    private void mapInitialize() {
        mapView.initialize();
        WMSParam wmsParam = new WMSParam().setUrl(WMSUrl).addParam("LAYERS", "raster");
        mapView.setWMSParam(wmsParam);
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            mapView.setCenter(new Coordinate(50.0650887, 19.9245536));
            mapView.setZoom(17);
        });
        setHandlersMap();
    }

    private void setHandlersMap() {
        mapView.addEventHandler(MapViewEvent.MAP_RIGHTCLICKED, event -> {
            if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC) {
                event.consume();
                Marker newMarker = Marker.createProvided(Marker.Provided.GREEN).setPosition(new Coordinate(event.getCoordinate().getLatitude(), event.getCoordinate().getLongitude())).setVisible(true);
                markerList.add(newMarker);
                mapView.addMarker(newMarker);
                List<String[]> markerData = new ArrayList<>();
                markerData.add(new String[]{String.valueOf(newMarker.getPosition().getLatitude()), String.valueOf(newMarker.getPosition().getLongitude()), "marker"});
                try {
                    Utils.saveGpsToCsv(markerData);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                generateTrace();
                if (markerList.size() > 1) {
                    expectedCourse.setText(String.valueOf(determineCourseBetweenTwoWaypoints(markerList.get(0).getPosition(), newMarker.getPosition())));
                }
            }
        });
    }

    public void clearMap() {
        removeAllMarkersAndLinesWithoutBoatPosition();
    }

    public void generateTrace() {
        if (markerList.size() > 1) {
            coordinateLine = null;
            coordinateLine = new CoordinateLine(markerList.stream().map(
                    marker1 -> new Coordinate(marker1.getPosition().getLatitude(), marker1.getPosition().getLongitude())
            ).collect(Collectors.toList()));
            coordinateLine.setColor(Color.RED);
            coordinateLine.setVisible(true);
            mapView.addCoordinateLine(coordinateLine);
            coordinateLines.add(coordinateLine);
        }
    }

    public void generateTraceFromBoatPosition(double latitude, double longitude) {
        Marker newMarker = Marker.createProvided(Marker.Provided.BLUE).setPosition(new Coordinate(latitude, longitude)).setVisible(true);
        if (foundBoatPosition) {
            mapView.removeMarker(markerList.get(0));
            markerList.remove(0);
            markerList.add(0, newMarker);
        } else {
            markerList.add(0, newMarker);
        }
        mapView.addMarker(newMarker);
        generateTrace();
        foundBoatPosition = true;
        mapView.setCenter(new Coordinate(latitude, longitude));
        if (markerList.size() > 1) {
            this.expectedCourse.setText(String.valueOf(determineCourseBetweenTwoWaypoints(newMarker.getPosition(), markerList.get(1).getPosition())));
        } else {
            this.expectedCourse.setText("-");
        }
    }

    public void changeMapTypeToOSM() {
        mapView.setMapType(MapType.OSM);
    }

    public void changeMapTypeToWMSMap() {
        mapView.setMapType(MapType.WMS);
    }

    public Boolean getFoundBoatPosition() {
        return this.foundBoatPosition;
    }

    public boolean designatedWaypoints() {
        if (foundBoatPosition) {
            return markerList.size() > 1;
        } else {
            return !markerList.isEmpty();
        }
    }

    public void removeAllMarkersAndLinesWithoutBoatPosition() {
        coordinateLines.forEach((coordinateLine1 -> mapView.removeCoordinateLine(coordinateLine1)));
        coordinateLines.clear();
        if (foundBoatPosition) {
            markerList.subList(1, markerList.size()).forEach(marker -> mapView.removeMarker(marker));
            markerList.subList(1, markerList.size()).clear();
        } else {
            markerList.forEach(marker -> mapView.removeMarker(marker));
            markerList.clear();
        }
    }

    public List<Marker> getDesignatedWaypoints() {
        return markerList.subList(1, markerList.size());
    }

    public void setCurrentBoatPositionWhileRunning(double latitude, double longitude) {
        // TODO: zmiany na czas testowania: zamiast nadpisywania akutalnej juz lokalizacji lodzi to nakladanie na mape wszystkich
//        if (currentBoatPositionWhileRunning != null) {
//            mapView.removeMarker(currentBoatPositionWhileRunning);
//        }
        currentBoatPositionWhileRunning = Marker.createProvided(Marker.Provided.RED).setPosition(new Coordinate(latitude, longitude)).setVisible(true);
        mapView.addMarker(currentBoatPositionWhileRunning);
        mapView.setCenter(new Coordinate(latitude, longitude));
    }

    public void clearCurrentBoatPositionAfterFinishedLastWaypoint() {
        mapView.removeMarker(currentBoatPositionWhileRunning);
        removeAllMarkersAndLinesWithoutBoatPosition();
        if (currentBoatPositionWhileRunning != null) {
            generateTraceFromBoatPosition(currentBoatPositionWhileRunning.getPosition().getLatitude(), currentBoatPositionWhileRunning.getPosition().getLongitude());
        }
        currentBoatPositionWhileRunning = null;
    }

    private double determineCourseBetweenTwoWaypoints(Coordinate firstCoordinate, Coordinate secondCoordinate) {
        double latitude1 = Math.toRadians(firstCoordinate.getLatitude());
        double latitude2 = Math.toRadians(secondCoordinate.getLatitude());
        double longDiff = Math.toRadians(secondCoordinate.getLongitude() - firstCoordinate.getLongitude());
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
