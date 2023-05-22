package com.example.systemobslugilodzizdalniesterowanej;

import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OSM {
    MapView mapView;
    List<Marker> markerList = new ArrayList<>();
    CoordinateLine coordinateLine = null;
    List<CoordinateLine> coordinateLines = new ArrayList<>();

    public OSM(MapView mapView) {
        this.mapView = mapView;
        mapInitialize();
    }

    private void mapInitialize() {
        mapView.initialize();
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            mapView.setCenter(new Coordinate(50.0650887, 19.9245536));
            mapView.setZoom(17);
        });
        setHandlersMap();
    }

    private void setHandlersMap() {
        mapView.addEventHandler(MapViewEvent.MAP_RIGHTCLICKED, event -> {
            event.consume();
            System.out.println("Event: map right clicked at: " + event.getCoordinate().getLongitude());
            markerList.add(Marker.createProvided(Marker.Provided.RED).setPosition(new Coordinate(event.getCoordinate().getLatitude(), event.getCoordinate().getLongitude())).setVisible(true));
            mapView.addMarker(markerList.get(markerList.size() - 1));
            if (markerList.size() > 1) {
                coordinateLine = new CoordinateLine(markerList.stream().map(marker1 -> new Coordinate(marker1.getPosition().getLatitude(), marker1.getPosition().getLongitude())
                ).collect(Collectors.toList()));
                coordinateLine.setColor(Color.RED);
                coordinateLine.setVisible(true);
                mapView.addCoordinateLine(coordinateLine);
                coordinateLines.add(coordinateLine);
            }
        });
    }

    public void clearMap() {
        coordinateLines.forEach((coordinateLine1 -> mapView.removeCoordinateLine(coordinateLine1)));
        coordinateLines.clear();
        markerList.forEach((marker -> mapView.removeMarker(marker)));
        markerList.clear();
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
