package com.example.systemobslugilodzizdalniesterowanej.boatmodel.components;

public class Lighting {
    private int power;
    private boolean changed;

    public Lighting() {
        power = 0;
        changed = false;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getPower() {
        return this.power;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public boolean getChanged() {
        return changed;
    }
}

