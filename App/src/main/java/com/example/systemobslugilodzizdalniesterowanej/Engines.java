package com.example.systemobslugilodzizdalniesterowanej;

public class Engines {
    private int motorOne;
    private int motorTwo;
    public boolean x;

    public Engines(){
        motorOne=0;
        motorTwo=0;
        x=false;
    }

    public void movingForward(){
        motorOne=80;
        motorTwo=-80;
    }

    public void movingBack(){
        motorOne=-80;
        motorTwo=80;
    }

    public void turnLeft(){
        motorTwo=-80;
        motorOne=0;
    }

    public void turnRight(){
        motorOne=80;
        motorTwo=0;
    }

    public void turnOff() {
        motorOne=0;
        motorTwo=0;
    }

    public double getMotorOne() {
        return motorOne;
    }

    public double getMotorTwo() {
        return motorTwo;
    }
}
