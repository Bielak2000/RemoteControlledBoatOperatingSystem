# Master's Thesis Project: Autonomous Boat Control System

## Overview

This project presents an integrated system designed to control a boat remotely via a radio module. The system consists of two main components:

- **Microcontroller firmware** responsible for handling communication with the boat's hardware.
- **Java desktop application** developed using JavaFX, providing a user-friendly interface for control and navigation.

## Features

### 1. Module, system selection and method of determining the model state

- **Radio Module Port Selection:**  
  The application allows users to select the communication port connected to the radio module, enabling flexible hardware configuration.

- **Operating System Selection:**  
  Users can specify the operating system environment to ensure compatibility and optimize communication.

- **Boat State Estimation Method Selection:**  
  Upon startup, the user can choose the method used to determine the boat's current state, enhancing flexibility in navigation and control accuracy.  
  One of the available methods employs a **Kalman filter algorithm** for improved state estimation and noise reduction.

### 2. Boat Control Modes

- **Manual Control:**  
  Users can manually steer the boat within the application, using intuitive controls to navigate the vessel in real time.

- **Automatic Control:**  
  The system supports autonomous navigation by allowing users to define a route on an interactive map. The boat will then follow the designated path automatically.

## Technical Details

- **Microcontroller Software:**  
  Written in **C**, the firmware interfaces with the radio module and additionally supports GPS and IMU modules for enhanced navigation and state estimation. It processes commands and controls boat actuators based on instructions received from the Java application.

- **Java/JavaFX Application:**  
  Provides a graphical user interface (GUI) for configuration, manual steering, route planning, and status monitoring. The application handles serial communication with the microcontroller through the selected port.

## Summary

This project combines embedded systems programming with desktop application development to create a versatile and efficient boat control system. It enables both direct manual operation and autonomous navigation, showcasing integration of hardware communication with modern user interface design.
