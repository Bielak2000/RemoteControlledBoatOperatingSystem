package com.example.systemobslugilodzizdalniesterowanej.boatmodel.components;

public class Flaps {
    private int firstFlap;
    private int secondFlap;
    private boolean changed;

    public Flaps() {
        firstFlap = 0;
        secondFlap = 0;
        changed = false;
    }

    public int getFirstFlap() {
        return firstFlap;
    }

    public int getSecondFlap() {
        return secondFlap;
    }

    public boolean getChanged() {
        return changed;
    }

    public void setChanged(boolean temp1) {
        this.changed = temp1;
    }

    public void onLeftFlap() {
        this.firstFlap = 1;
    }

    public void onRightFlap() {
        this.secondFlap = 1;
    }

    public void offLeftFlap() {
        this.firstFlap = 0;
    }

    public void offRightFlap() {
        this.secondFlap = 0;
    }
}
