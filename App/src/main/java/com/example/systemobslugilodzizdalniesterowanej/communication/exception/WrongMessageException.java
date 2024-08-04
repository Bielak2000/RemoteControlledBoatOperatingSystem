package com.example.systemobslugilodzizdalniesterowanej.communication.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WrongMessageException extends RuntimeException {

    private static String COMMUNICAT = "The wrong received message from boat: %s";

    public WrongMessageException(String message) {
        super(String.format(COMMUNICAT, message));
        log.error(String.format("The wrong received message from boat: %s", message));
    }

}
