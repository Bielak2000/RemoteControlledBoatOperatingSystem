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
import javafx.application.Platform;
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
    private List<Marker> currentBoatPositionWhileRunningList = new ArrayList<>();
    private Marker currentBoatPositionWhileRunning = null;
    @Getter
    @Setter
    private int waypointIndex = 0;
    @Setter
    @Getter
    private Coordinate nextWaypointOnTheRoad = null;
    @Getter
    Label expectedCourse;
    @Getter
    @Setter
    private Double currentCourse = null;
    @Setter
    private Coordinate startWaypoint = null;

//    TODO: 29-30.03 - DZWOLA
//    @Getter
//    private Coordinate startTestingCoordinate = new Coordinate(50.6924563333333, 22.5879335);
//    private List<Coordinate> testingCoordinates = new ArrayList<>() {
//        {
//          add(new Coordinate(50.6925085454545, 22.5877821666667));
//          add(new Coordinate(50.6926097142857, 22.5878681428571));
//          add(new Coordinate(50.6925688, 22.588001));
//          add(startTestingCoordinate);
//        }
//    };


//    TODO: 03.04 - KRK
//    @Getter
//    private Coordinate startTestingCoordinate = new Coordinate(50.090715125, 19.8585824444444);
//
//    @Getter
//    private List<Coordinate> testingCoordinates = new ArrayList<>() {
//        {
//            add(new Coordinate(50.09082,19.858632));
//            add(new Coordinate(50.090786, 19.858814));
//            add(new Coordinate(50.090679, 19.858776));
//            add(startTestingCoordinate);
//        }

//    TODO: 04.04 - KRK
//@Getter
//private Coordinate startTestingCoordinate = new Coordinate(50.090721, 19.8585595);
//
//    @Getter
//    private List<Coordinate> testingCoordinates = new ArrayList<>() {
//        {
//            add(new Coordinate(50.090862, 19.858646));
//            add(new Coordinate(50.090815, 19.858858));
//            add(new Coordinate(50.090701, 19.858802));
//            add(startTestingCoordinate);
//        }

//  TODO: 05.04 - KRK
    @Getter
    private Coordinate startTestingCoordinate = new Coordinate(50.09074, 19.858576);

    @Getter
    private List<Coordinate> testingCoordinates = new ArrayList<>() {
        {
            add(new Coordinate(50.090775, 19.858675));
            add(new Coordinate(50.09086, 19.85865));
            add(new Coordinate(50.090893, 19.858755));
            add(new Coordinate(50.090865, 19.858816));
            add(new Coordinate(50.090805, 19.858839));
            add(new Coordinate(50.090778, 19.858789));
            add(new Coordinate(50.090755, 19.858829));
            add(new Coordinate(50.090736, 19.858748));
            add(new Coordinate(50.090672, 19.858782));
            add(startTestingCoordinate);
        }
    };

//    TODO: 06-04 wieczorem - KRK
//    @Getter
//    private Coordinate startTestingCoordinate = new Coordinate(50.09076, 19.858646);
//
//    @Getter
//    private List<Coordinate> testingCoordinates = new ArrayList<>() {
//        {
//            add(new Coordinate(50.090812,19.858711));
//            add(new Coordinate(50.090891, 19.858676));
//            add(new Coordinate(50.090901, 19.858805));
//            add(new Coordinate(50.090904, 19.858894));
//            add(new Coordinate(50.090807, 19.858941));
//            add(new Coordinate(50.090798, 19.858882));
//            add(new Coordinate(50.090751, 19.85889));
//            add(new Coordinate(50.090736, 19.858812));
//            add(new Coordinate(50.090683, 19.858788));
//            add(startTestingCoordinate);
//        }
//    };

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
                testingCoordinates.forEach(coordinate -> {
                    Marker newMarker = Marker.createProvided(Marker.Provided.GREEN).setPosition(coordinate).setVisible(true);
                    markerList.add(newMarker);
                    mapView.addMarker(newMarker);
                    generateTrace();
                });
            }
        });
    }

    public void clearMap() {
        removeAllMarkersAndLinesWithoutBoatPosition();
    }

    public void generateTrace() {
        if (markerList.size() > 1) {
            if (boatModeController.getBoatMode() != BoatMode.AUTONOMIC_RUNNING && !coordinateLines.isEmpty()) {
                CoordinateLine coordinateLineToRemove = coordinateLines.get(0);
                coordinateLines.remove(0);
                Platform.runLater(() -> {
                    mapView.removeCoordinateLine(coordinateLineToRemove);
                });
            }
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
            Platform.runLater(() -> {
                mapView.removeMarker(markerList.get(0));
                markerList.remove(0);
                markerList.add(0, newMarker);
            });
        } else {
            markerList.add(0, newMarker);
        }
        Platform.runLater(() -> {
            mapView.addMarker(newMarker);
            generateTrace();
            if (!foundBoatPosition) {
                mapView.setCenter(new Coordinate(latitude, longitude));
            }
            foundBoatPosition = true;
        });
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

    public void setCurrentBoatPositionWhileRunning(Coordinate coordinate) {
        if (currentBoatPositionWhileRunning != null) {
//            Platform.runLater(() -> {
//                mapView.removeMarker(currentBoatPositionWhileRunning);
//            });
        }
        currentBoatPositionWhileRunning = Marker.createProvided(Marker.Provided.RED).setPosition(coordinate).setVisible(true);
        currentBoatPositionWhileRunningList.add(currentBoatPositionWhileRunning);
        Platform.runLater(() -> {
            mapView.addMarker(currentBoatPositionWhileRunning);
//            mapView.setCenter(new Coordinate(latitude, longitude));
        });
    }

    public void clearCurrentBoatPositionAfterFinishedLastWaypoint() {
        if (!currentBoatPositionWhileRunningList.isEmpty()) {
            for (Marker marker : currentBoatPositionWhileRunningList) {
                Platform.runLater(() -> {
                    mapView.removeMarker(marker);
                });
            }
            currentBoatPositionWhileRunningList.clear();
        }
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
        Platform.runLater(() -> {
            this.expectedCourse.setText(expectedCourse);
        });
    }

    public void incrementWaypointIndex() {
        this.waypointIndex++;
    }

    public Coordinate getCurrentBoatPosition() {
        if (currentBoatPositionWhileRunning != null && boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING) {
            return currentBoatPositionWhileRunning.getPosition();
        }
        if (markerList.size() > 0) {
            return markerList.get(0).getPosition();
        } else {
            return null;
        }
    }

    public Coordinate getStartWaypoint() {
        if (startWaypoint != null) return startWaypoint;
        else if (markerList.size() > 0) return markerList.get(0).getPosition();
        else return null;
    }
}
