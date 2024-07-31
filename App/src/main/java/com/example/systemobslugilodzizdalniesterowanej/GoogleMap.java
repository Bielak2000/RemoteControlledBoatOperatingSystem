package com.example.systemobslugilodzizdalniesterowanej;

import com.dlsc.gmapsfx.GoogleMapView;
import com.dlsc.gmapsfx.MapComponentInitializedListener;
import com.dlsc.gmapsfx.javascript.object.DirectionsPane;
import com.dlsc.gmapsfx.javascript.object.LatLong;
import com.dlsc.gmapsfx.javascript.object.MapOptions;
import com.dlsc.gmapsfx.javascript.object.MapTypeIdEnum;
import com.dlsc.gmapsfx.javascript.object.Marker;
import com.dlsc.gmapsfx.javascript.object.MarkerOptions;
import com.dlsc.gmapsfx.service.directions.DirectionStatus;
import com.dlsc.gmapsfx.service.directions.DirectionsRenderer;
import com.dlsc.gmapsfx.service.directions.DirectionsResult;
import com.dlsc.gmapsfx.service.directions.DirectionsService;
import com.dlsc.gmapsfx.service.directions.DirectionsServiceCallback;

import java.util.ArrayList;
import java.util.List;

public class GoogleMap implements MapComponentInitializedListener, DirectionsServiceCallback {

    protected GoogleMapView mapView;
    protected com.dlsc.gmapsfx.javascript.object.GoogleMap map;
    protected Marker marker;
    protected List<Marker> markerList;
    protected MarkerOptions markerOptions;
    protected DirectionsService directionsService;
    protected DirectionsRenderer directionsRenderer = null;
    protected DirectionsPane directionsPane;

    public GoogleMap() {
        mapView = new GoogleMapView();
        mapView.addMapInitializedListener(this);
        markerOptions = new MarkerOptions();
        markerList = new ArrayList<>();
    }

    @Override
    public void mapInitialized() {
        MapOptions mapOptions = new MapOptions();
        mapOptions.mapType(MapTypeIdEnum.ROADMAP).overviewMapControl(false).zoom(18).keyboardShortcuts(false);
        map = mapView.createMap(mapOptions);
        LatLong latLong = new LatLong(50.0650887, 19.9245536);
        map.setCenter(latLong);

        Marker marker1 = new Marker(markerOptions);
        Marker marker2 = new Marker(markerOptions);
        marker1.setPosition(new LatLong(50.0750887, 19.9345536));
        marker2.setPosition(new LatLong(50.0850887, 19.9445536));
        map.addMarker(marker1);

//  Marker marker1 = new Marker(markerOptions);
//  Marker marker2 = new Marker(markerOptions);
//  Marker marker3 = new Marker(markerOptions);
//  Marker marker4 = new Marker(markerOptions);
//  marker1.setPosition(new LatLong(50.0750887,19.9345536));
//  marker2.setPosition(new LatLong(50.0850887,19.9445536));
//  marker3.setPosition(new LatLong(50.0950887,19.9545536));
//  marker4.setPosition(new LatLong(50.1050887,19.9645536));
//  map.addMarker(marker1);
//  map.addMarker(marker2);
//  map.addMarker(marker3);
//  map.addMarker(marker4);


    }

    @Override
    public void directionsReceived(DirectionsResult results, DirectionStatus status) {
        System.out.println(results);
        System.out.println(status);
    }

    public void setPosition(LatLong latLong) {
        marker.setPosition(latLong);
        map.setCenter(latLong);
    }

    public void addNewMarker(LatLong latLong) {
        Marker marker1 = new Marker(markerOptions);
        marker1.setPosition(latLong);
        markerList.add(marker1);
        map.addMarker(marker1);
        map.setCenter(latLong);
    }

    public GoogleMapView getMapView() {
        return mapView;
    }
}
