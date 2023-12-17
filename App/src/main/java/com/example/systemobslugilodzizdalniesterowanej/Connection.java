package com.example.systemobslugilodzizdalniesterowanej;

import com.sothawo.mapjfx.Marker;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;

import static jssc.SerialPort.MASK_RXCHAR;

import java.util.ArrayList;
import java.util.List;

public class Connection {
    private BoatModeController boatModeController;
    private SerialPort serialPort;
    private Engines engines;
    private Lighting lighting;
    private Flaps flaps;
    private List<String> portNames = new ArrayList<>();
    private Label connectionStatus;
    private Label lightPower;
    private Boolean networkStatus;
    private OSMMap osmMap;
    private Stage stage;

    public Connection(Engines engines, Lighting lighting, Flaps flaps, Label connectionStatus, Label lightPower, Boolean networkStatus, OSMMap osmMap, Stage stage, BoatModeController boatModeController) {
        com.fazecast.jSerialComm.SerialPort[] ports = com.fazecast.jSerialComm.SerialPort.getCommPorts();
        for (com.fazecast.jSerialComm.SerialPort port : ports) {
            portNames.add(port.getSystemPortName());
        }
        this.engines = engines;
        this.lighting = lighting;
        this.flaps = flaps;
        this.connectionStatus = connectionStatus;
        this.lightPower = lightPower;
        this.networkStatus = networkStatus;
        this.osmMap = osmMap;
        this.stage = stage;
        this.boatModeController = boatModeController;
    }

    public void connect(String port, String system) {
        try {

            if (system.equals("Windows"))
                serialPort = new SerialPort(port);
            else
                serialPort = new SerialPort("/dev/" + port);

            serialPort.openPort();
            connectionStatus.setText("Polaczono z radionadajnikiem!");
            serialPort.setParams(SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setEventsMask(MASK_RXCHAR);

            serialPort.addEventListener((SerialPortEvent serialPortEvent) -> {
                if (serialPortEvent.isRXCHAR()) {
                    try {
                        String readString = serialPort.readString();
                        String[] array = readString.split("_");

                        if (array.length > 0) {
                            lighting.setPower(Integer.parseInt(array[0]));
                            System.out.println("Oswietlenie: " + array[0]);
                        }
                        String[] localization = {"", ""};
                        if (array.length > 1) {
                            localization = array[1].split(",");
                            System.out.println("Lokalizacja: " + array[1]);
                        }

                        setLightPowerLabel();
                        setBoatPositionOnMap(localization);
                    } catch (SerialPortException ex) {
                        System.out.println("Problem z odbiorem danych: " + ex);
                    }
                }
            });
        } catch (SerialPortException serialPortException) {
            connectionStatus.setTextFill(Color.color(1, 0, 0));
            connectionStatus.setText("Brak polaczenia z radionadajnikiem!");
            dialogWarning("Brak polaczenia", "Aplikacja nie moze sie polaczyc z radionadajnikiem!");
            stage.close();
        }
    }

    public void sendParameters() {
        try {
            if (boatModeController.getBoatMode() == BoatMode.KEYBOARD_CONTROL) {
                String sentInfo = ("0" + "_" + String.valueOf((int) engines.getMotorOne()) + "_" + String.valueOf((int) engines.getMotorTwo()) + "_"
                        + String.valueOf((int) lighting.getPower()) + "_"
                        + String.valueOf((int) flaps.getFirstFlap()) + "_" + flaps.getSecondFlap() + "_");
                serialPort.writeString(sentInfo);

                System.out.println("Wyslano: " + sentInfo);
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    public void sendChangedBoatModeAndWaypoints() {
        try {
            String sendInfo = "1";
            serialPort.writeString(sendInfo);
            Thread.sleep(1000);
            List<Marker> markerList = osmMap.getDesignatedWaypoints();
            String isLastMarker = "0";
            for (int i = 0; i < markerList.size(); i++) {
                if (i == markerList.size() - 1) {
                    isLastMarker = "1";
                }
                sendInfo = "1" + "_" + markerList.get(i).getPosition().getLatitude().toString() + "_" + markerList.get(i).getPosition().getLongitude().toString() + isLastMarker;
                serialPort.writeString(sendInfo);
                System.out.println("Wyslano waypoint: lat - " + markerList.get(i).getPosition().getLatitude().toString() + ", long - " + markerList.get(i).getPosition().getLongitude().toString());
                Thread.sleep(1000);
            }

            System.out.println("Wyslano waypointy");
        } catch (SerialPortException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void setBoatPositionOnMap(String[] localization) {
        if (networkStatus) {
            String[] finalLocalization = localization;
            Platform.runLater(() -> {
                if (!finalLocalization[0].startsWith("INV") && !finalLocalization[0].equals("") && !finalLocalization[0].isEmpty()) {
                    // TODO: dane lokalizacyjne przychodzace z lodzi, za pierwszym razem lub w trybie nie autonomicznym
                    // TODO: ma to byc dodane na pcozatek listy markerow i wygenerowac trase,
                    //  za kazdym kolejnym razem ma byc pole kotre bedzie to przedstawiac bez usuwania trasy i poczatkowej lokalizacji
                    if (boatModeController.getBoatMode() != BoatMode.AUTONOMIC_RUNNING) {
                        osmMap.generateTraceFromBoatPosition(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1]));
                    } else {
                        osmMap.setCurrentBoatPositionWhileRunning(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1]));
                    }
//                                    controller.getMap().addNewMarker(new LatLong(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1])));
//                                    controller.getMap().setPosition(new LatLong(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1])));
                }
            });
        }
    }

    private void setLightPowerLabel() {
        Platform.runLater(() -> {
            lightPower.setText(String.valueOf(lighting.getPower() + "%"));
        });
    }

    private void dialogWarning(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(text);
        alert.showAndWait();
    }
}
