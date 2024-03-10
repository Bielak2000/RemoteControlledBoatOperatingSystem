package com.example.systemobslugilodzizdalniesterowanej;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;

public class BoatModeController {
    private static BoatModeController boatModeController;
    private static String RUNNING_BOAT_INFORMATION = "Łódka rozpoczeła pływanie po waypointach.";
    private static String STARTING_BOAT_INFORMATION = "Łódka rozpoczyna pływanie po waypointach, proszę czekać ...";
    private BoatMode boatMode;
    private Button leftFlap;
    private Button lightDown;
    private Label lightPower;
    private Button lightUp;
    private Button moveDown;
    private Button moveLeft;
    private Button moveRight;
    private Button moveUp;
    private Button rightFlap;
    private Label lightingText;
    private Label flapsText;
    private Button startSwimming;
    private Button clearTrace;
    private ToggleButton modeChooser;
    private Button exit;
    private Label runningBoatInformation;
    private Button stopSwimmingButton;

    // TODO: do testu
    private Label gpsCourse;
    private Label expectedCourse;
    private Label sensorCourse;
    @FXML
    private Label gpsCourseText;
    @FXML
    private Label expectedCourseText;
    @FXML
    private Label sensorCourseText;



    private BoatModeController(Button leftFlap, Button lightDown, Label lightPower, Button lightUp, Button moveDown, Button moveLeft, Button moveRight, Button moveUp,
                               Button rightFlap, Label lightingText, Label flapsText, Button startSwimming, Button clearTrace, ToggleButton modeChooser, Button exit,
                               Label runningBoatInformation, Button stopSwimmingButton, Label gpsCourse, Label expectedCourse, Label sensorCourse, Label gpsCourseText,
                               Label sensorCourseText, Label expectedCourseText) {
        this.boatMode = BoatMode.KEYBOARD_CONTROL;
        this.leftFlap = leftFlap;
        this.lightDown = lightDown;
        this.lightPower = lightPower;
        this.lightUp = lightUp;
        this.moveDown = moveDown;
        this.moveLeft = moveLeft;
        this.moveRight = moveRight;
        this.moveUp = moveUp;
        this.rightFlap = rightFlap;
        this.lightingText = lightingText;
        this.flapsText = flapsText;
        this.startSwimming = startSwimming;
        this.clearTrace = clearTrace;
        this.modeChooser = modeChooser;
        this.exit = exit;
        this.runningBoatInformation = runningBoatInformation;
        this.stopSwimmingButton = stopSwimmingButton;
        this.gpsCourse = gpsCourse;
        this.expectedCourse = expectedCourse;
        this.sensorCourse = sensorCourse;
        this.gpsCourseText = gpsCourseText;
        this.expectedCourseText = expectedCourseText;
        this.sensorCourseText = sensorCourseText;
    }

    public static BoatModeController getInstance(Button leftFlap, Button lightDown, Label lightPower, Button lightUp, Button moveDown, Button moveLeft, Button moveRight,
                                                 Button moveUp, Button rightFlap, Label lightingText, Label flapsText, Button startSwimming, Button clearTrace,
                                                 ToggleButton modeChooser, Button exit, Label runningBoatInformation, Button stopSwimmingButton,Label gpsCourse, Label expectedCourse, Label sensorCourse,
                                                 Label gpsCourseText,
                                                 Label sensorCourseText, Label expectedCourseText) {
        if (boatModeController == null) {
            boatModeController = new BoatModeController(leftFlap, lightDown, lightPower, lightUp, moveDown, moveLeft, moveRight, moveUp,
                    rightFlap, lightingText, flapsText, startSwimming, clearTrace, modeChooser, exit, runningBoatInformation, stopSwimmingButton,
                    gpsCourse, expectedCourse, sensorCourse, gpsCourseText, sensorCourseText, expectedCourseText);
        }
        return boatModeController;
    }

    public BoatMode getBoatMode() {
        return this.boatMode;
    }

    public void setBoatMode(BoatMode boatMode) {
        if (boatMode == BoatMode.AUTONOMIC) {
            setViewForAutonomicBoatMode();
        } else if (boatMode == BoatMode.KEYBOARD_CONTROL) {
            if (this.boatMode == BoatMode.AUTONOMIC_RUNNING) {
                stopSwimmingButton.setVisible(false);
                setRunningBoatInformation(STARTING_BOAT_INFORMATION, Color.color(230, 0, 0), false);
            }
            enableActionsForAutonomicRunningBoatMode();
            setViewForKeyboardControlBoatMode();
        } else if (boatMode == BoatMode.AUTONOMIC_STARTING) {
            blockActionsForAutonomicRunningBoatMode();
        } else if (boatMode == BoatMode.AUTONOMIC_RUNNING) {
            setRunningBoatInformation(RUNNING_BOAT_INFORMATION, Color.color(81, 181, 61), true);
            stopSwimmingButton.setVisible(true);
        }
        this.boatMode = boatMode;
    }

    private void setRunningBoatInformation(String text, Color color, boolean visible) {
        this.runningBoatInformation.setVisible(visible);
        this.runningBoatInformation.setText(text);
        this.runningBoatInformation.setTextFill(color);
    }

    private void setViewForAutonomicBoatMode() {
        lightingText.setVisible(false);
        lightDown.setVisible(false);
        lightUp.setVisible(false);
        flapsText.setVisible(false);
        leftFlap.setVisible(false);
        rightFlap.setVisible(false);
        moveDown.setVisible(false);
        moveLeft.setVisible(false);
        moveRight.setVisible(false);
        moveUp.setVisible(false);
        startSwimming.setVisible(true);
        clearTrace.setVisible(true);
        gpsCourse.setVisible(true);
        sensorCourse.setVisible(true);
        expectedCourse.setVisible(true);
        gpsCourseText.setVisible(true);
        sensorCourseText.setVisible(true);
        expectedCourseText.setVisible(true);
    }

    private void setViewForKeyboardControlBoatMode() {
        lightingText.setVisible(true);
        lightDown.setVisible(true);
        lightUp.setVisible(true);
        flapsText.setVisible(true);
        leftFlap.setVisible(true);
        rightFlap.setVisible(true);
        moveDown.setVisible(true);
        moveLeft.setVisible(true);
        moveRight.setVisible(true);
        moveUp.setVisible(true);
        startSwimming.setVisible(false);
        clearTrace.setVisible(false);
        gpsCourse.setVisible(false);
        sensorCourse.setVisible(false);
        expectedCourse.setVisible(false);
        gpsCourseText.setVisible(false);
        sensorCourseText.setVisible(false);
        expectedCourseText.setVisible(false);
    }

    private void blockActionsForAutonomicRunningBoatMode() {
        modeChooser.setDisable(true);
        startSwimming.setDisable(true);
        clearTrace.setDisable(true);
        exit.setDisable(true);
    }

    private void enableActionsForAutonomicRunningBoatMode() {
        modeChooser.setDisable(false);
        startSwimming.setDisable(false);
        clearTrace.setDisable(false);
        exit.setDisable(false);
    }
}
