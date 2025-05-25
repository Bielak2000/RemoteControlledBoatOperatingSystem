package com.example.systemobslugilodzizdalniesterowanej.boatmodel.components;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.LinearAndAngularSpeed;
import com.example.systemobslugilodzizdalniesterowanej.common.Utils;

public class Engines {

    private static int MAX_LEFT_ENGINE_PERCENTAGE_POWER = 50;
    private static int MAX_RIGHT_ENGINE_PERCENTAGE_POWER = 80;
    private static double ANGULAR_FACTORY = 0.7;

    private int motorLeft;
    private int motorRight;
    private boolean temp;

    public Engines() {
        motorLeft = 0;
        motorRight = 0;
        temp = false;
    }

    public void movingForward() {
        motorLeft = MAX_LEFT_ENGINE_PERCENTAGE_POWER;
        motorRight = -1 * MAX_RIGHT_ENGINE_PERCENTAGE_POWER;
    }

    public void movingBack() {
        motorLeft = -1 * MAX_LEFT_ENGINE_PERCENTAGE_POWER;
        motorRight = MAX_RIGHT_ENGINE_PERCENTAGE_POWER;
    }

    public void turnLeft() {
        motorRight = -1 * MAX_RIGHT_ENGINE_PERCENTAGE_POWER;
        motorLeft = 0;
    }

    public void turnRight() {
        motorLeft = MAX_LEFT_ENGINE_PERCENTAGE_POWER;
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
                this.motorLeft = mapLeftEngineValue(Math.round((linearAndAngularSpeed.getAngularSpeed()) / 2));
                this.motorRight = -1 * ((int) Math.round(-(linearAndAngularSpeed.getAngularSpeed()) / 2));
            } else {
                this.motorLeft = 0;
                this.motorRight = 0;
            }
        } else {
            if (linearAndAngularSpeed.getAngularSpeed() != 0) {
                double leftValue = (linearAndAngularSpeed.getLinearSpeed() + ((linearAndAngularSpeed.getAngularSpeed() * ANGULAR_FACTORY))) / (1.0 + ANGULAR_FACTORY);
                double rightValue = (linearAndAngularSpeed.getLinearSpeed() - ((linearAndAngularSpeed.getAngularSpeed() * ANGULAR_FACTORY))) / (1.0 + ANGULAR_FACTORY);
                this.motorLeft = mapLeftEngineValue(leftValue);
                this.motorRight = -1 * ((int) rightValue);
            } else {
                this.motorLeft = mapLeftEngineValue(linearAndAngularSpeed.getLinearSpeed());
                this.motorRight = -1 * ((int) linearAndAngularSpeed.getLinearSpeed());
            }
        }
        this.temp = true;
    }

    private int mapLeftEngineValue(double autonomicValue) {
        return (int) (autonomicValue * (MAX_LEFT_ENGINE_PERCENTAGE_POWER / Utils.MAX_LINEAR_SPEED_PERCENTAGE));
    }
}
