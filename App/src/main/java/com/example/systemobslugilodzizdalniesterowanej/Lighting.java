package com.example.systemobslugilodzizdalniesterowanej;

public class Lighting {
    private int power;
    private boolean temp;

    public Lighting(){
        power=0;
        temp =false;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getPower(){
        return this.power;
    }

    public void setTemp(boolean temp) {
        this.temp = temp;
    }
    public boolean getTemp() {
        return temp;
    }
}

