package com.example.systemobslugilodzizdalniesterowanej.maps;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import com.sothawo.mapjfx.MapType;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.WMSParam;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OSMMap {
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static String WMSUrl = "https://mapy.geoportal.gov.pl/wss/service/PZGIK/ORTO/WMS/HighResolution";
    private MapView mapView;
    public List<Marker> markerList = new ArrayList<>();
    private CoordinateLine coordinateLine = null;
    private List<CoordinateLine> coordinateLines = new ArrayList<>();
    private BoatModeController boatModeController;
    private Boolean foundBoatPosition;
    private Marker currentBoatPositionWhileRunning = null;
    @Getter
    @Setter
    private int waypointIndex = 0;
    @Setter
    @Getter
    private Coordinate nextWaypointOnTheRoad = null;
    Label expectedCourse;
    @Getter
    @Setter
    private Double currentCourse = null;

    /**
     * GREEN TAG - waypoint determined by user
     * RED TAG - waypoint determined by boat during autonomic swimming
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
            mapView.setZoom(19);
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
                generateTrace();
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
        if (currentBoatPositionWhileRunning != null) {
            mapView.removeMarker(currentBoatPositionWhileRunning);
        }
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

    public void setExpectedCourse(Coordinate firstCoordinate, Coordinate secondCoordinate) {
        this.expectedCourse.setText(String.valueOf(Utils.determineCourseBetweenTwoWaypoints(firstCoordinate, secondCoordinate)));
    }

    public void setExpectedCourse(String expectedCourse) {
        this.expectedCourse.setText(expectedCourse);
    }

    public void incrementWaypointIndex() {
            this.waypointIndex++;
    }

    public Coordinate getCurrentBoatPosition() {
        return markerList.get(0).getPosition();
    }
}
