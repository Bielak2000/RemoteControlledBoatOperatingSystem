package com.example.systemobslugilodzizdalniesterowanej;

import com.dlsc.gmapsfx.MapComponentInitializedListener;
import com.dlsc.gmapsfx.javascript.object.*;
import com.dlsc.gmapsfx.GoogleMapView;

public class Map implements MapComponentInitializedListener {

 GoogleMapView mapView;
 GoogleMap map;
 Marker marker;

 public Map(){
  mapView = new GoogleMapView();
  mapView.addMapInitializedListener(this);
 }

 @Override
 public void mapInitialized() {
  MapOptions mapOptions = new MapOptions();
  mapOptions.mapType(MapTypeIdEnum.ROADMAP).zoom(16).keyboardShortcuts(false);
  map = mapView.createMap(mapOptions);
  LatLong latLong = new LatLong(50.0650887,19.9245536);
  MarkerOptions markerOptions=new MarkerOptions();
  marker = new Marker(markerOptions);
  marker.setPosition(latLong);
  map.addMarker(marker);
  map.setCenter(latLong);
 }

 public void setPosition(LatLong latLong){
  marker.setPosition(latLong);
  map.setCenter(latLong);
 }

 public GoogleMapView getMapView() {
  return mapView;
 }
}
