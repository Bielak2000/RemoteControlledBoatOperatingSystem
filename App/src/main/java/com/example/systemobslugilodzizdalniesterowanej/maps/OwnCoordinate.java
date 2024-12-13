package com.example.systemobslugilodzizdalniesterowanej.maps;

import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.sothawo.mapjfx.Coordinate;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class OwnCoordinate {

    double x, y;

    public OwnCoordinate(Coordinate coordinate, Coordinate startPoint) {
        double distance = Utils.calculateDistance(startPoint, coordinate);
        double angle = Utils.determineCourseBetweenTwoWaypoints(startPoint, coordinate);
        double transformAngle = (450.0 - angle) % 360.0;
        this.x = distance * Math.cos(transformAngle);
        this.y = distance * Math.sin(transformAngle);
    }

    public Coordinate transformCoordinateToGlobalCoordinateSystem(Coordinate startPoint) {
        // Konwersja szerokości geograficznej z stopni na radiany
        double lat1Rad = Math.toRadians(startPoint.getLatitude());
        // Przesunięcie szerokości geograficznej
        double deltaLat = this.y / Utils.EARTH_RADIUS_M;
        double lat2 = startPoint.getLatitude() + Math.toDegrees(deltaLat);
        // Przesunięcie długości geograficznej
        double deltaLon = this.x / (Utils.EARTH_RADIUS_M * Math.cos(lat1Rad));
        double lon2 = startPoint.getLongitude() + Math.toDegrees(deltaLon);
        return new Coordinate(lat2, lon2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OwnCoordinate that = (OwnCoordinate) o;
        return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
