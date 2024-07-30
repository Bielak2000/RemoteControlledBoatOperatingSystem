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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jssc.SerialPort.MASK_RXCHAR;

public class Connection {
    private static String KEYBOARD_CONTROL_MODE_MARKING = "0";
    private static String AUTONOMOUS_MODE_MARKING = "1";
    private static String STOP_SWIMMING_MARKING = "2";
    private static int MILLISECONDS_TIME_BETWEEN_SEND_INFORMATION = 2000;
    private static int BOAT_IS_SWIMMING_BY_WAYPOINTS = 0;
    private static int BOAT_FINISHED_SWIMMING_BY_WAYPOINTS = 1;
    private static int BOAT_MANUALLY_FINISHED_SWIMMING_BY_WAYPOINTS = 2;
    private static String BOAT_RUNNING_SWIMMING_INFORMATION = "Łódka porszua się po wyznaczonych punktach. Nie wyłączaj aplikacji i nie wykonuj żadnych czynności, czekaj na informację z łodzi o uzyskaniu docelowej pozycji.";
    private static String BOAT_FINISHED_SWIMMING_INFORMATION = "Łódka dopłyneła do ostaniego waypointa, zmieniono tryb sterowania na tryb manualny. Jeśli chcesz ponownie wyznaczyć trasę wykonaj odpowiednie czynności jak poprzednio.";
    private static String BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION = "Ręcznie przerwano pływanie łodzi po waypointach, zmieniono tryb sterowania na tryb manualny.";
    private ExecutorService executorService;
    private BoatModeController boatModeController;
    private ProgressDialogController progressDialogController;
    private SerialPort serialPort;
    private Engines engines;
    private Lighting lighting;
    private Flaps flaps;
    private List<String> portNames = new ArrayList<>();
    private Label connectionStatus;
    private Label lightPower;

    // TODO: do testow
    private Label gpsCourse;
    private Label sensorCourse;
    private Label expectedCourse;

    private Boolean networkStatus;
    private OSMMap osmMap;
    private Stage stage;
    private Label runningBoatInformation;

    public Connection(Engines engines, Lighting lighting, Flaps flaps, Label connectionStatus, Label lightPower, Boolean networkStatus, OSMMap osmMap,
                      Stage stage, BoatModeController boatModeController, Label runningBoatInformation,
                      Label gpsCourse, Label sensorCourse, Label expectedCourse) {
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
        this.executorService = Executors.newFixedThreadPool(1);
        this.runningBoatInformation = runningBoatInformation;

        this.gpsCourse = gpsCourse;
        this.sensorCourse = sensorCourse;
        this.expectedCourse = expectedCourse;
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
                        System.out.println("Przyszło: " + readString);
                        String[] array = readString.split("_");

//                        if (array.length > 0) {
//                            lighting.setPower(Integer.parseInt(array[0]));
//                            System.out.println("Oswietlenie: " + array[0]);
//                        }
//                        String[] localization = {"", ""};
//                        if (array.length > 1) {
//                            localization = array[1].split(",");
//                            System.out.println("Lokalizacja: " + array[1]);
//                        }
//                        if (array.length > 2) {
//                            if (Integer.parseInt(array[2]) == BOAT_IS_SWIMMING_BY_WAYPOINTS) {
//                                if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING) {
//                                    boatModeController.setBoatMode(BoatMode.AUTONOMIC_RUNNING);
//                                    runningBoatInformation.setVisible(true);
//                                    Platform.runLater(() -> showInformationDialog("Łódka rozpoczeła pływanie", BOAT_RUNNING_SWIMMING_INFORMATION, 700));
//                                }
//                            } else if (Integer.parseInt(array[2]) == BOAT_FINISHED_SWIMMING_BY_WAYPOINTS) {
//                                Platform.runLater(() -> {
//                                    boatModeController.setBoatMode(BoatMode.KEYBOARD_CONTROL);
//                                    // TODO: przetestowac czy uda sie wyczyscic mape
//                                    osmMap.clearCurrentBoatPositionAfterFinishedLastWaypoint();
//                                    showInformationDialog("Łódka osiągneła punkt docelowy", BOAT_FINISHED_SWIMMING_INFORMATION, 700);
//                                });
//                            } else if (Integer.parseInt(array[2]) == BOAT_MANUALLY_FINISHED_SWIMMING_BY_WAYPOINTS) {
//                                Platform.runLater(() -> {
//                                    boatModeController.setBoatMode(BoatMode.KEYBOARD_CONTROL);
//                                    // TODO: przetestowac czy uda sie wyczyscic mape
//                                    osmMap.clearCurrentBoatPositionAfterFinishedLastWaypoint();
//                                    showInformationDialog("Przerwano pływanie łodzi", BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION, 500);
//                                });
//                            }
//                        }

                        // TODO: do testow
                        if (array.length == 3) {
                            String[] localization = {"", ""};
                            if (array.length > 0) {
                                if (!array[0].equals("-1") && !array[0].equals("INVALID")) {
                                    localization = array[0].split(",");
                                    if(localization.length==2) {
                                        System.out.println("Lokalizacja: " + localization[0] + ", " + localization[1]);
                                        setBoatPositionOnMap(localization);
                                    }
                                }
                            }
                            if (array.length > 1) {
                                Platform.runLater(() -> {
                                    if (!array[1].equals("-1") && !array[1].equals("INVALID")) {
                                        sensorCourse.setText(array[1]);
                                        System.out.println("Sensor course: " + array[1]);
                                    }
                                });
                            }
                            if (array.length > 2) {
                                Platform.runLater(() -> {
                                    if (!array[2].equals("-1") && !array[2].equals("INVALID")) {
                                        gpsCourse.setText(array[2]);
                                        System.out.println("Gps course: " + array[2]);
                                    }
                                });
                            }
                            List<String[]> courseData = new ArrayList<>();
                            courseData.add(new String[]{sensorCourse.getText(), gpsCourse.getText(), expectedCourse.getText()});
                            Utils.saveCourseToCsv(courseData, "course-with-x-calibration-two-waypoints-1.csv");
                        }
                        // TODO: koniec testow

//                          setLightPowerLabel();
//                            if(localization.length==2) {
//                              setBoatPositionOnMap(localization);
//                            }
                    } catch (SerialPortException ex) {
                        System.out.println("Problem z odbiorem danych: " + ex);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            });
        } catch (SerialPortException serialPortException) {
            connectionStatus.setTextFill(Color.color(1, 0, 0));
            connectionStatus.setText("Brak połączenia z radionadajnikiem!");
            dialogWarning("Brak połączenia", "Aplikacja nie może się połączyć z radionadajnikiem!");
            stage.close();
        }
    }

    public void sendParameters() {
        try {
            if (boatModeController.getBoatMode() == BoatMode.KEYBOARD_CONTROL) {
                String sentInfo = (KEYBOARD_CONTROL_MODE_MARKING + "_"
                        + String.valueOf((int) engines.getMotorOne()) + "_" + String.valueOf((int) engines.getMotorTwo()) + "_"
                        + String.valueOf((int) lighting.getPower()) + "_"
                        + String.valueOf((int) flaps.getFirstFlap()) + "_" + flaps.getSecondFlap() + "_");
                serialPort.writeString(sentInfo);

                System.out.println("Wyslano: " + sentInfo);
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    public void asyncSendChangedBoatModeAndWaypoints() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String sendInfo;
                    List<Marker> markerList = osmMap.getDesignatedWaypoints();
                    String isLastMarker = "0";
                    for (int i = 0; i < markerList.size(); i++) {
                        if (i == markerList.size() - 1) {
                            isLastMarker = "1";
                        }
                        sendInfo = AUTONOMOUS_MODE_MARKING + "_"
                                + markerList.get(i).getPosition().getLatitude().toString() + "_"
                                + markerList.get(i).getPosition().getLongitude().toString() + "_"
                                + isLastMarker + "_";
                        serialPort.writeString(sendInfo);
                        System.out.println("Wyslano waypoint: lat - "
                                + markerList.get(i).getPosition().getLatitude().toString()
                                + ", long - " + markerList.get(i).getPosition().getLongitude().toString());
                        Thread.sleep(MILLISECONDS_TIME_BETWEEN_SEND_INFORMATION);
                    }

                    System.out.println("Wyslano waypointy");
                    Platform.runLater(() -> runningBoatInformation.setVisible(true));
                } catch (SerialPortException e) {
                    connectionStatus.setTextFill(Color.color(1, 0, 0));
                    connectionStatus.setText("Brak polaczenia z radionadajnikiem!");
                    dialogWarning("Brak polaczenia", "Aplikacja nie moze sie polaczyc z radionadajnikiem!");
                    stage.close();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Platform.runLater(() -> progressDialogController.closeProgressDialogController());
            }
        });
    }

    public void sendStopSwimmingInfo() {
        try {
            if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING) {
                String sentInfo = STOP_SWIMMING_MARKING;
                serialPort.writeString(sentInfo);

                System.out.println("Wyslano: " + sentInfo);
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    public void setProgressDialogController(ProgressDialogController progressDialogController) {
        this.progressDialogController = progressDialogController;
    }

    // TODO: do testow
//    boolean firstWaypoint = true;

    private void setBoatPositionOnMap(String[] localization) {
        if (networkStatus) {
            String[] finalLocalization = localization;
            Platform.runLater(() -> {
                if (!finalLocalization[0].startsWith("INV") && !finalLocalization[0].equals("") && !finalLocalization[0].isEmpty()) {
                    // TODO: dane lokalizacyjne przychodzace z lodzi, za pierwszym razem lub w trybie nie autonomicznym
                    // TODO: ma to byc dodane na pcozatek listy markerow i wygenerowac trase,
                    //  za kazdym kolejnym razem ma byc pole kotre bedzie to przedstawiac bez usuwania trasy i poczatkowej lokalizacji
                    if (boatModeController.getBoatMode() != BoatMode.AUTONOMIC_STARTING) {
//                    List<String[]> dataLines = new ArrayList<>();
//                    if (firstWaypoint) {
//                        dataLines.add(new String[]{finalLocalization[0], finalLocalization[1], "first localization"});
                        // TODO: nadpisuje pierwsza pozycje
                        // TODO: to zostaje przed testami
                        osmMap.generateTraceFromBoatPosition(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1]));
//                        firstWaypoint = false;
                    } else {
                        // TODO: dodaje kolejne
                        // TODO: to zostaje przed testami
                        osmMap.setCurrentBoatPositionWhileRunning(Double.parseDouble(finalLocalization[0]), Double.parseDouble(finalLocalization[1]));
//                        dataLines.add(new String[]{finalLocalization[0], finalLocalization[1], "new localization"});
                    }
//                    try {
//                        Utils.saveGpsToCsv(dataLines);
//                    } catch (FileNotFoundException e) {
//                        throw new RuntimeException(e);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
                }
            });
        }
    }
    // TODO: koniec testow

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

    private void showInformationDialog(String title, String text, int width) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(text);
        alert.getDialogPane().setMaxWidth(width);
        alert.showAndWait();
    }
}
