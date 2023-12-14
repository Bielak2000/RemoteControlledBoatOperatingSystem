package com.example.systemobslugilodzizdalniesterowanej;

import com.dlsc.gmapsfx.javascript.object.LatLong;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;

import static jssc.SerialPort.MASK_RXCHAR;

import java.util.ArrayList;
import java.util.List;

public class Connection {
    private SerialPort serialPort;
    private SystemController controller;
    private Engines engines;
    private Lighting lighting;
    private Flaps flaps;
    private List<String> portNames = new ArrayList<>();

    public Connection(SystemController controller1, Engines engines1, Lighting lighting1, Flaps flaps1) {
        com.fazecast.jSerialComm.SerialPort[] ports = com.fazecast.jSerialComm.SerialPort.getCommPorts();
        for (com.fazecast.jSerialComm.SerialPort port : ports) {
            portNames.add(port.getSystemPortName());
        }
        controller = controller1;
        engines = engines1;
        lighting = lighting1;
        flaps = flaps1;
    }

    public void connect(String port, String system) {
        try {

            if (system.equals("Windows"))
                serialPort = new SerialPort(port);
            else
                serialPort = new SerialPort("/dev/" + port);

            serialPort.openPort();
            controller.getConnectionStatus().setText("Polaczono z radionadajnikiem!");
            serialPort.setParams(SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setEventsMask(MASK_RXCHAR);

            serialPort.addEventListener((SerialPortEvent serialPortEvent) -> {
                if (serialPortEvent.isRXCHAR()) {
                    try {
                        String[] localization = {"", ""};
                        String b = serialPort.readString();
                        String[] array = b.split("_");
                        if (array.length > 0) {
                            lighting.setPower(Integer.parseInt(array[0]));
                            System.out.println("Oswietlenie: " + array[0]);
                        }
                        if (array.length > 1) {
                            localization = array[1].split(",");
                            System.out.println("Lokalizacja: " + array[1]);
                        }


                        Platform.runLater(() -> {
                            controller.getLightPower().setText(String.valueOf(lighting.getPower() + "%"));
                        });
                        if (controller.getNetworkStatus()) {
                            String[] finalLocalization = localization;
                            Platform.runLater(() -> {
                                if (!finalLocalization[0].startsWith("INV") && !finalLocalization[0].equals("") && !finalLocalization[0].isEmpty()) {
                                    // TODO: dane lokalizacyjne przychodzace z lodzi, za pierwszym razem lub w trybie nie autonomicznym
                                    // TODO: ma to byc dodane na pcozatek listy markerow i wygenerowac trase,
                                    //  za kazdym kolejnym razem ma byc pole kotre bedzie to przedstawiac bez usuwania trasy i poczatkowej lokalizacji
                                    controller.changeBoatPosition(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1]));
//                                    controller.getMap().addNewMarker(new LatLong(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1])));
//                                    controller.getMap().setPosition(new LatLong(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1])));
                                }
                            });
                        }
                    } catch (SerialPortException ex) {
                        System.out.println("Problem z odbiorem danych: " + ex);
                    }
                }
            });
        } catch (SerialPortException serialPortException) {
            controller.getConnectionStatus().setTextFill(Color.color(1, 0, 0));
            controller.getConnectionStatus().setText("Brak polaczenia z radionadajnikiem!");
            controller.dialogNotConnect("Brak polaczenia", "Aplikacja nie moze sie polaczyc z radionadajnikiem!");
            controller.getStage().close();
        }
    }

    public void sendParameters() {
        try {
            String sentInfo = (String.valueOf((int) engines.getMotorOne()) + "_" + String.valueOf((int) engines.getMotorTwo()) + "_"
                    + String.valueOf((int) lighting.getPower()) + "_"
                    + String.valueOf((int) flaps.getFirstFlap()) + "_" + flaps.getSecondFlap() + "_");
            serialPort.writeString(sentInfo);

            System.out.println("Wyslano: " + sentInfo);
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }
}
