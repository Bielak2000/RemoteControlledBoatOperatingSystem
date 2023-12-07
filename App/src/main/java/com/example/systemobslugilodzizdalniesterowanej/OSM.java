package com.example.systemobslugilodzizdalniesterowanej;

import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OSM {
    private static String WMSUrl = "https://mapy.geoportal.gov.pl/wss/service/PZGIK/ORTO/WMS/HighResolution";
    MapView mapView;
    List<Marker> markerList = new ArrayList<>();
    CoordinateLine coordinateLine = null;
    List<CoordinateLine> coordinateLines = new ArrayList<>();
    BoatMode boatMode;
    Boolean foundBoatPosition;

    public OSM(MapView mapView) {
        this.mapView = mapView;
        this.boatMode = BoatMode.KEYBOARD_CONTROL;
        this.foundBoatPosition = false;
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
            if (boatMode == BoatMode.AUTONOMIC) {
                event.consume();
                Marker newMarker = Marker.createProvided(Marker.Provided.RED).setPosition(new Coordinate(event.getCoordinate().getLatitude(), event.getCoordinate().getLongitude())).setVisible(true);
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
        // trzeba zrobic inwersje, ten marker ma trafiac na poczatek tej listy
        if(foundBoatPosition) {
            mapView.removeMarker(markerList.get(0));
            markerList.remove(0);
            markerList.add(0, newMarker);
        } else {
            markerList.add(newMarker);
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

    public void setBoatMode(BoatMode boatMode) {
        this.boatMode = boatMode;
        if(boatMode == BoatMode.KEYBOARD_CONTROL) {
            removeAllMarkersAndLinesWithoutBoatPosition();
        }
    }

    private void removeAllMarkersAndLinesWithoutBoatPosition() {
        coordinateLines.forEach((coordinateLine1 -> mapView.removeCoordinateLine(coordinateLine1)));
        coordinateLines.clear();
        if(foundBoatPosition) {
            markerList.subList(1, markerList.size()).forEach(marker -> mapView.removeMarker(marker));
            markerList.subList(1, markerList.size()).clear();
        } else {
            markerList.forEach(marker -> mapView.removeMarker(marker));
            markerList.clear();
        }
    }

//        public boolean generateTrace() {
//        if (markerList.size() > 1) {
//            clearCoordinateLine();
//            coordinateLine = new CoordinateLine(markerList.stream().map(marker1 -> new Coordinate(marker1.getPosition().getLatitude(), marker1.getPosition().getLongitude())
//            ).collect(Collectors.toList()));
//            coordinateLine.setColor(Color.RED);
//            coordinateLine.setVisible(true);
//            mapView.addCoordinateLine(coordinateLine);
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    private void clearCoordinateLine() {
//        if (coordinateLine != null) {
//            mapView.removeCoordinateLine(coordinateLine);
//            coordinateLine = null;
//        }
//    }
}
