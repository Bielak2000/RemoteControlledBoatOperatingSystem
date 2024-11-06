package com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class LinearAndAngularSpeed {

    private double angularSpeed;
    private double linearSpeed;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinearAndAngularSpeed that = (LinearAndAngularSpeed) o;
        return Double.compare(angularSpeed, that.angularSpeed) == 0 && Double.compare(linearSpeed, that.linearSpeed) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(angularSpeed, linearSpeed);
    }

}
