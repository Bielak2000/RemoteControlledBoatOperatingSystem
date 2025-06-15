package com.example.systemobslugilodzizdalniesterowanej.boatmodel.keyboardcontrol;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Engines;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Flaps;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Lighting;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.communication.Connection;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;

public class KeyboardHandler {
    private Scene scene;
    private BoatModeController boatModeController;
    private Connection connection;
    private Lighting lighting;
    private Engines engines;
    private Flaps flaps;
    private Button moveUp;
    private Button moveDown;
    private Button moveLeft;
    private Button moveRight;
    private Button leftFlap;
    private Button rightFlap;
    private Button lightDown;
    private Button lightUp;

    public KeyboardHandler(Scene scene, Connection connection, BoatModeController boatModeController, Button moveUp, Button moveDown,
                           Button moveLeft, Button moveRight, Button leftFlap, Button rightFlap, Button lightDown, Button lightUp,
                           Engines engines, Lighting lighting, Flaps flaps) {
        this.scene = scene;
        this.connection = connection;
        this.boatModeController = boatModeController;
        this.engines = engines;
        this.lighting = lighting;
        this.flaps = flaps;
        this.moveUp = moveUp;
        this.moveDown = moveDown;
        this.moveLeft = moveLeft;
        this.moveRight = moveRight;
        this.leftFlap = leftFlap;
        this.rightFlap = rightFlap;
        this.lightDown = lightDown;
        this.lightUp = lightUp;
    }

    public void keyboardHandler() {
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (boatModeController.getBoatMode() == BoatMode.KEYBOARD_CONTROL) {
                    switch (keyEvent.getCode()) {
                        case UP:
                            moveUp.arm();
                            engines.movingForward();
                            if (!engines.getChanged()) {
                                connection.sendParameters();
                                engines.setChanged(true);
                            }
                            break;
                        case DOWN:
                            moveDown.arm();
                            engines.movingBack();
                            if (!engines.getChanged()) {
                                connection.sendParameters();
                                engines.setChanged(true);
                            }
                            break;
                        case LEFT:
                            moveLeft.arm();
                            engines.turnLeft();
                            if (!engines.getChanged()) {
                                connection.sendParameters();
                                engines.setChanged(true);
                            }
                            break;
                        case RIGHT:
                            moveRight.arm();
                            engines.turnRight();
                            if (!engines.getChanged()) {
                                connection.sendParameters();
                                engines.setChanged(true);
                            }
                            break;
                        case T:
                            leftFlap.arm();
                            flaps.onLeftFlap();
                            if (!flaps.getChanged()) {
                                connection.sendParameters();
                                flaps.setChanged(true);
                            }
                            break;
                        case Y:
                            rightFlap.arm();
                            flaps.onRightFlap();
                            if (!flaps.getChanged()) {
                                connection.sendParameters();
                                flaps.setChanged(true);
                            }
                            break;
                        case Q:
                            lightDown.arm();
                            if (!lighting.getChanged()) {
                                lighting.setPower(0);
                                connection.sendParameters();
                                lighting.setChanged(true);
                            }
                            break;
                        case E:
                            lightUp.arm();
                            if (!lighting.getChanged()) {
                                lighting.setPower(100);
                                connection.sendParameters();
                                lighting.setChanged(true);
                            }
                            break;
                    }
                    keyEvent.consume();
                }
            }
        });

        scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (boatModeController.getBoatMode() == BoatMode.KEYBOARD_CONTROL) {
                    switch (keyEvent.getCode()) {
                        case UP:
                            moveUp.disarm();
                            engines.turnOff();
                            connection.sendParameters();
                            engines.setChanged(false);
                            break;
                        case DOWN:
                            moveDown.disarm();
                            engines.turnOff();
                            connection.sendParameters();
                            engines.setChanged(false);
                            break;
                        case LEFT:
                            moveLeft.disarm();
                            engines.turnOff();
                            connection.sendParameters();
                            engines.setChanged(false);
                            break;
                        case RIGHT:
                            moveRight.disarm();
                            engines.turnOff();
                            connection.sendParameters();
                            engines.setChanged(false);
                            break;
                        case T:
                            leftFlap.disarm();
                            flaps.offLeftFlap();
                            flaps.setChanged(false);
                            break;
                        case Y:
                            rightFlap.disarm();
                            flaps.offRightFlap();
                            flaps.setChanged(false);
                            break;
                        case Q:
                            lightDown.disarm();
                            lighting.setPower(-1);
                            connection.sendParameters();
                            lighting.setChanged(false);
                            break;
                        case E:
                            lightUp.disarm();
                            lighting.setPower(-1);
                            connection.sendParameters();
                            lighting.setChanged(false);
                            break;
                    }
                }
                keyEvent.consume();
            }
        });
    }

    public void stopBoat() {
        moveLeft.disarm();
        moveRight.disarm();
        moveDown.disarm();
        moveUp.disarm();
        engines.turnOff();
        engines.setChanged(false);

        leftFlap.disarm();
        rightFlap.disarm();
        flaps.offLeftFlap();
        flaps.offRightFlap();
        flaps.setChanged(false);

        lightUp.disarm();
        lightDown.disarm();
        lighting.setPower(-1);
        lighting.setChanged(false);

        connection.sendParameters();
    }
}
