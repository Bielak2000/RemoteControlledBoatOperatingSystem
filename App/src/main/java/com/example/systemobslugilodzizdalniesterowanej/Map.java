package com.example.systemobslugilodzizdalniesterowanej;

import com.dlsc.gmapsfx.MapComponentInitializedListener;
import com.dlsc.gmapsfx.javascript.object.*;
import com.dlsc.gmapsfx.GoogleMapView;
import com.dlsc.gmapsfx.service.directions.DirectionsService;

import java.util.ArrayList;
import java.util.List;

public class Map implements MapComponentInitializedListener {

 GoogleMapView mapView;
 GoogleMap map;
 Marker marker;
 List<Marker> markerList;
 MarkerOptions markerOptions;
 DirectionsService directionsService;

 public Map(){
  mapView = new GoogleMapView();
  mapView.addMapInitializedListener(this);
  markerOptions = new MarkerOptions();
  markerList = new ArrayList<>();
 }

 @Override
 public void mapInitialized() {
  MapOptions mapOptions = new MapOptions();
  mapOptions.mapType(MapTypeIdEnum.ROADMAP).zoom(18).keyboardShortcuts(false);
  map = mapView.createMap(mapOptions);
  LatLong latLong = new LatLong(50.0650887,19.9245536);
//  marker = new Marker(markerOptions);
//  marker.setPosition(latLong);
//  map.addMarker(marker);
  map.panTo(latLong);
  directionsService = new DirectionsService();
  directionsService.getRoute();
  new Poly

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

  map.setCenter(latLong);
 }

 public void setPosition(LatLong latLong){
  marker.setPosition(latLong);
  map.setCenter(latLong);
 }

 public void addNewMarker(LatLong latLong){
  Marker marker1 = new Marker(markerOptions);
  marker1.setPosition(latLong);
  markerList.add(marker1);
  map.addMarker(marker1);
  map.setCenter(latLong);
//  if(markerList.size()>1) {
//   map.panTo();
//  }
 }

 public GoogleMapView getMapView() {
  return mapView;
 }
}
