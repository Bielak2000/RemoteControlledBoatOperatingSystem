package com.example.systemobslugilodzizdalniesterowanej.boatmodel.components;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.LinearAndAngularSpeed;

public class Engines {

    private static double ANGULAR_FACTORY = 0.5;

    private int motorLeft;
    private int motorRight;
    private boolean temp;

    public Engines() {
        motorLeft = 0;
        motorRight = 0;
        temp = false;
    }

    public void movingForward() {
        motorLeft = 80;
        motorRight = -80;
    }

    public void movingBack() {
        motorLeft = -80;
        motorRight = 80;
    }

    public void turnLeft() {
        motorRight = -80;
        motorLeft = 0;
    }

    public void turnRight() {
        motorLeft = 80;
        motorRight = 0;
    }

    public void turnOff() {
        motorLeft = 0;
        motorRight = 0;
    }

    public double getMotorLeft() {
        return motorLeft;
    }

    public double getMotorRight() {
        return motorRight;
    }

    public void setTemp(boolean temp) {
        this.temp = temp;
    }

    public boolean getTemp() {
        return temp;
    }

    public void setEnginesPowerByAngularAndLinearSpeed(LinearAndAngularSpeed linearAndAngularSpeed) {
        if (linearAndAngularSpeed.getLinearSpeed() == 0) {
            if (linearAndAngularSpeed.getAngularSpeed() != 0) {
                this.motorLeft = (int) Math.round((linearAndAngularSpeed.getAngularSpeed()) / 2);
                this.motorRight = (int) Math.round(-(linearAndAngularSpeed.getAngularSpeed()) / 2);
            } else {
                this.motorLeft = 0;
                this.motorRight = 0;
            }
        } else {
            double leftValue = (linearAndAngularSpeed.getLinearSpeed() + ((linearAndAngularSpeed.getAngularSpeed() * ANGULAR_FACTORY))) / (1.0 + ANGULAR_FACTORY);
            double rightValue = (linearAndAngularSpeed.getLinearSpeed() - ((linearAndAngularSpeed.getAngularSpeed() * ANGULAR_FACTORY))) / (1.0 + ANGULAR_FACTORY);
            this.motorLeft = (int) leftValue;
            this.motorRight = (int) rightValue;
        }
        this.temp = true;
    }
}
