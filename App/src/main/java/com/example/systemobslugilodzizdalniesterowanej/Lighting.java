package com.example.systemobslugilodzizdalniesterowanej;

public class Lighting {
    private int power;
    public boolean x;

    public Lighting(){
        power=0;
        x=false;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getPower(){
        return this.power;
    }
}

