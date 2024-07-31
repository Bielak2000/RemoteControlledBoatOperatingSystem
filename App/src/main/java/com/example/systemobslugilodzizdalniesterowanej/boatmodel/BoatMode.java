package com.example.systemobslugilodzizdalniesterowanej.boatmodel;

/**
 * KEYBOARD_CONTROL - control boat by keyboard
 * AUTONOMIC - first mode in autonomic swimming
 * AUTONOMIC_STARTING - second mode in autonomic mode: sent data to boat
 * AUTONOMIC_RUNNING - third mode in autonomic mode: boat is swimming by waypoints
 */
public enum BoatMode {
    KEYBOARD_CONTROL, AUTONOMIC, AUTONOMIC_STARTING, AUTONOMIC_RUNNING;
}
