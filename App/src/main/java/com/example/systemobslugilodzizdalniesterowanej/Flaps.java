package com.example.systemobslugilodzizdalniesterowanej;

public class Flaps {
    private int firstFlap;
    private int secondFlap;
    private boolean temp;

    public Flaps(){
        firstFlap=0;
        secondFlap=0;
        temp=false;
    }

    public int getFirstFlap() {
        return firstFlap;
    }

    public int getSecondFlap() {
        return secondFlap;
    }
    public boolean getTemp() {return temp;}
    public void setTemp(boolean temp1){
        this.temp=temp1;
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
