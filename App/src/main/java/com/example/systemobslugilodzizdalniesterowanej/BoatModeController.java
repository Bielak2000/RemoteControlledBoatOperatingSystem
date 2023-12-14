package com.example.systemobslugilodzizdalniesterowanej;

public class BoatModeController {
    private static BoatModeController boatModeController;
    private BoatMode boatMode;

    private BoatModeController() {
        this.boatMode = BoatMode.KEYBOARD_CONTROL;
    }

    public static BoatModeController getInstance() {
        if (boatModeController == null) {
            boatModeController = new BoatModeController();
        }
        return boatModeController;
    }

    public BoatMode getBoatMode() {
        return this.boatMode;
    }

    public void setBoatMode(BoatMode boatMode) {
        this.boatMode = boatMode;
    }
}
