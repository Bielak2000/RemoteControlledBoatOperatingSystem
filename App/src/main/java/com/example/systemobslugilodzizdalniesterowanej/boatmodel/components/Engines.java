package com.example.systemobslugilodzizdalniesterowanej.boatmodel.components;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.LinearAndAngularSpeed;

public class Engines {
    private int motorOne;
    private int motorTwo;
    private boolean temp;

    public Engines() {
        motorOne = 0;
        motorTwo = 0;
        temp = false;
    }

    public Engines(int motorOne, int motorTwo) {
        this.motorOne = motorOne;
        this.motorTwo = motorTwo;
        this.temp = true;
    }

    public void movingForward() {
        motorOne = 80;
        motorTwo = -80;
    }

    public void movingBack() {
        motorOne = -80;
        motorTwo = 80;
    }

    public void turnLeft() {
        motorTwo = -80;
        motorOne = 0;
    }

    public void turnRight() {
        motorOne = 80;
        motorTwo = 0;
    }

    public void turnOff() {
        motorOne = 0;
        motorTwo = 0;
    }

    public double getMotorOne() {
        return motorOne;
    }

    public double getMotorTwo() {
        return motorTwo;
    }

    public void setTemp(boolean temp) {
        this.temp = temp;
    }

    public boolean getTemp() {
        return temp;
    }

    // TODO: mapowanie liniowej i katowej na moc silnikow
    public void setEnginesPowerByAngularAndLinearSpeed(LinearAndAngularSpeed linearAndAngularSpeed) {
        this.motorOne = 50;
        this.motorTwo = 50;
        this.temp = true;
    }
}
