package com.example.systemobslugilodzizdalniesterowanej.communication;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Engines;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Flaps;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Lighting;
import com.example.systemobslugilodzizdalniesterowanej.communication.exception.WrongMessageException;
import com.example.systemobslugilodzizdalniesterowanej.controllers.ProgressDialogController;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.Marker;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.calculateDistance;
import static jssc.SerialPort.MASK_RXCHAR;

@Slf4j
public class Connection {

    // MESSAGE TO BOAT
    private final static String FROM_APP_KEYBOARD_CONTROL_MODE_MARKING = "0";
    private final static String FROM_APP_MOVE_TO_AUTONOMIC_MODE = "1";
    private final static String FROM_APP_AUTONOMOUS_MODE_MARKING = "2";
    private final static String FROM_APP_AUTONOMOUS_MODE_STOP_SENDING_WAYPOINT = "3";
    private final static String FROM_APP_STOP_SWIMMING_MARKING = "4";

    // MESSAGE FROM BOAT
    private final static int FROM_BOAT_LIGHTING_MESSAGE = 0;
    private final static int FROM_BOAT_GPS_MESSAGE = 1;
    private final static int FROM_BOAT_BOAT_IS_SWIMMING_BY_WAYPOINTS = 2;
    private final static int FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS = 3;
    private final static int FROM_BOAT_BOAT_MANUALLY_FINISHED_SWIMMING_BY_WAYPOINTS = 4;

    private final static int MILLISECONDS_TIME_BETWEEN_SEND_INFORMATION = 2000;
    private final static String BOAT_RUNNING_SWIMMING_INFORMATION = "Łódka porszua się po wyznaczonych punktach. Nie wyłączaj aplikacji i nie wykonuj żadnych czynności, czekaj na informację z łodzi o uzyskaniu docelowej pozycji. Możesz zastopować łódź przyciskiem STOP.";
    private final static String BOAT_FINISHED_SWIMMING_INFORMATION = "Łódka dopłyneła do ostaniego waypointa, zmieniono tryb sterowania na tryb manualny. Jeśli chcesz ponownie wyznaczyć trasę wykonaj odpowiednie czynności jak poprzednio.";
    private final static String BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION = "Ręcznie przerwano pływanie łodzi po waypointach, zmieniono tryb sterowania na tryb manualny.";

    // TODO: do testow
    private final static int FROM_BOAT_GPS_COURSE_MESSAGE = 5;
    private final static int FROM_BOAT_SENSOR_COURSE_MESSAGE = 6;

    private ExecutorService executorService;
    private BoatModeController boatModeController;
    @Setter
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

    private Boolean networkStatus;
    private OSMMap osmMap;
    private Stage stage;
    private Label runningBoatInformation;

    public Connection(Engines engines, Lighting lighting, Flaps flaps, Label connectionStatus, Label lightPower, Boolean networkStatus, OSMMap osmMap,
                      Stage stage, BoatModeController boatModeController, Label runningBoatInformation,
                      Label gpsCourse, Label sensorCourse) {
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
    }

    public void connect(String port, String system) {
        try {
            if (system.equals("Windows")) {
                serialPort = new SerialPort(port);
            } else {
                serialPort = new SerialPort("/dev/" + port);
            }
            serialPort.openPort();
            connectionStatus.setText("Polaczono z radionadajnikiem!");
            serialPort.setParams(SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setEventsMask(MASK_RXCHAR);

            serialPort.addEventListener((SerialPortEvent serialPortEvent) -> {
                if (serialPortEvent.isRXCHAR()) {
                    try {
                        String readString = serialPort.readString();
                        log.info("Received: {}", readString);
                        String[] array = readString.split("_");

                        if (checkCorrectlyReceivedData(array)) {
                            int messageSign = Integer.parseInt(array[0]);
                            receivedMessageHandler(messageSign, array);
                        } else {
                            log.error(String.format("The wrong received message from boat: %s", readString));
                        }
                    } catch (SerialPortException ex) {
                        log.error("Error while receiving data: {}", ex.getMessage());
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
                String sentInfo = (FROM_APP_KEYBOARD_CONTROL_MODE_MARKING + "_"
                        + String.valueOf((int) engines.getMotorOne()) + "_" + String.valueOf((int) engines.getMotorTwo()) + "_"
                        + String.valueOf((int) lighting.getPower()) + "_"
                        + String.valueOf((int) flaps.getFirstFlap()) + "_" + flaps.getSecondFlap() + "_");
                serialPort.writeString(sentInfo);
                log.info("Sent: {}", sentInfo);
            }
        } catch (SerialPortException e) {
            log.error("Error while sending message: {}", e.getMessage());
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
                    for (Marker marker : markerList) {
                        sendInfo = FROM_APP_AUTONOMOUS_MODE_MARKING + "_"
                                + marker.getPosition().getLatitude().toString() + "_"
                                + marker.getPosition().getLongitude().toString() + "_";
                        serialPort.writeString(sendInfo);
                        log.info("Sent waypoint: lat - {}, long - {}", marker.getPosition().getLatitude().toString(), marker.getPosition().getLongitude().toString());
                        Thread.sleep(MILLISECONDS_TIME_BETWEEN_SEND_INFORMATION);
                    }
                    osmMap.setNextWaypointOnTheRoad(markerList.get(0).getPosition());
                    log.info("Sent all waypoints");
                    sendInfo = FROM_APP_AUTONOMOUS_MODE_STOP_SENDING_WAYPOINT;
                    serialPort.writeString(sendInfo);
                    log.info("Sent confirmation of sent waypoints");

                    Platform.runLater(() -> runningBoatInformation.setVisible(true));

                    // Symulacja rozpoczecia plywania lodki
//                    Thread.sleep(3000);
//                    if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_STARTING) {
//                        boatModeController.setBoatMode(BoatMode.AUTONOMIC_RUNNING);
//                        runningBoatInformation.setVisible(true);
//                        Platform.runLater(() -> showInformationDialog("Łódka rozpoczeła pływanie", BOAT_RUNNING_SWIMMING_INFORMATION, 700));
//                    }

                } catch (SerialPortException e) {
                    connectionStatus.setTextFill(Color.color(1, 0, 0));
                    connectionStatus.setText("Brak polaczenia z radionadajnikiem!");
                    dialogWarning("Brak polaczenia", "Aplikacja nie moze sie polaczyc z radionadajnikiem!");
                    stage.close();
                } catch (InterruptedException e) {
                    log.error("Error while async sending changed boat mode and waypoints: {}", e.getMessage());
                }
                Platform.runLater(() -> progressDialogController.closeProgressDialogController());
            }
        });
    }

    public void sendStopSwimmingInfo() {
        try {
            String sentInfo = FROM_APP_STOP_SWIMMING_MARKING;
            serialPort.writeString(sentInfo);
            log.info("Sent message: {}", sentInfo);
        } catch (SerialPortException e) {
            log.error("Error while sending stop swimming info: {}", e.getMessage());
        }
    }

    public void sendMoveToAutonomicInfo() {
        try {
            if (boatModeController.getBoatMode() == BoatMode.KEYBOARD_CONTROL) {
                String sentInfo = FROM_APP_MOVE_TO_AUTONOMIC_MODE;
                serialPort.writeString(sentInfo);
                log.info("Sent: {}", sentInfo);
            }
        } catch (SerialPortException e) {
            log.error("Error while sending move to autonomic info: {}", e.getMessage());
        }
    }

    private void receivedMessageHandler(int messageSign, String[] array) {
        switch (messageSign) {
            case FROM_BOAT_LIGHTING_MESSAGE:
                lighting.setPower(Integer.parseInt(array[1]));
                setLightPowerLabel();
                log.info("New light power: {}", array[1]);
                break;
            case FROM_BOAT_GPS_MESSAGE:
                String[] localization = {"", ""};
                if (!array[1].equals("-1") && !array[1].equals("INVALID")) {
                    localization = array[1].split(",");
                    log.info("New localization: {}", array[1]);
                }
                if (localization.length == 2) {
                    setBoatPositionOnMap(localization);
                }
                if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING) {
                    Coordinate newPosition = new Coordinate(Double.parseDouble(localization[0]), Double.parseDouble(localization[1]));
                    if (calculateDistance(newPosition, osmMap.getNextWaypointOnTheRoad()) < 3) {
                        osmMap.incrementWaypointIndex();
                        List<Marker> markerList = osmMap.getDesignatedWaypoints();
                        if (osmMap.getWaypointIndex() < markerList.size()) {
                            osmMap.setNextWaypointOnTheRoad(markerList.get(osmMap.getWaypointIndex()).getPosition());
                            osmMap.setExpectedCourse(new Coordinate(Double.parseDouble(localization[0]), Double.parseDouble(localization[1])), osmMap.getNextWaypointOnTheRoad());
                        }
                    } else {
                        osmMap.setExpectedCourse(new Coordinate(Double.parseDouble(localization[0]), Double.parseDouble(localization[1])), osmMap.getNextWaypointOnTheRoad());
                    }
                }
                break;
            case FROM_BOAT_BOAT_IS_SWIMMING_BY_WAYPOINTS:
                if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_STARTING) {
                    boatModeController.setBoatMode(BoatMode.AUTONOMIC_RUNNING);
                    runningBoatInformation.setVisible(true);
                    Platform.runLater(() -> showInformationDialog("Łódka rozpoczeła pływanie", BOAT_RUNNING_SWIMMING_INFORMATION, 700));
                } else {
                    throw new WrongMessageException("Received that boat is swimming by waypoints but current boat mode isn't AUTONOMIC_STARTING");
                }
                break;
            case FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS:
                Platform.runLater(() -> {
                    boatModeController.setBoatMode(BoatMode.KEYBOARD_CONTROL);
                    // TODO: przetestowac czy uda sie wyczyscic mape
                    osmMap.clearCurrentBoatPositionAfterFinishedLastWaypoint();
                    showInformationDialog("Łódka osiągneła punkt docelowy", BOAT_FINISHED_SWIMMING_INFORMATION, 700);
                });
                osmMap.setNextWaypointOnTheRoad(null);
                osmMap.setWaypointIndex(0);
                break;
            case FROM_BOAT_BOAT_MANUALLY_FINISHED_SWIMMING_BY_WAYPOINTS:
                Platform.runLater(() -> {
                    boatModeController.setBoatMode(BoatMode.KEYBOARD_CONTROL);
                    // TODO: przetestowac czy uda sie wyczyscic mape
                    osmMap.clearCurrentBoatPositionAfterFinishedLastWaypoint();
                    Platform.runLater(() -> progressDialogController.closeProgressDialogController());
                    showInformationDialog("Przerwano pływanie łodzi", BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION, 500);
                });
                osmMap.setNextWaypointOnTheRoad(null);
                osmMap.setWaypointIndex(0);
                break;
            case FROM_BOAT_GPS_COURSE_MESSAGE:
                Platform.runLater(() -> {
                    this.gpsCourse.setText(array[1]);
                });
                break;
            case FROM_BOAT_SENSOR_COURSE_MESSAGE:
                Platform.runLater(() -> {
                    this.sensorCourse.setText(array[1]);
                });
                break;
        }
    }

    /**
     * Funkcja nakladajaca lokalizacje lodki na mape po poprwanym odczycie
     * Jesli lodz jest w trybie autonomicznym to nadpisuje lokalizacje lodki jakie uzyskuje
     * Jesli lodz jest w domyslnym trybie to ustawia aktualne polozenie
     *
     * @param localization - lokalizacja lodki
     */
    private void setBoatPositionOnMap(String[] localization) {
        if (networkStatus) {
            Platform.runLater(() -> {
                if (!localization[0].startsWith("INV") && !localization[0].equals("") && !localization[0].isEmpty()) {
                    BoatMode currentBoatMode = boatModeController.getBoatMode();
                    if (currentBoatMode != BoatMode.AUTONOMIC_STARTING && currentBoatMode != BoatMode.AUTONOMIC_RUNNING) {
                        osmMap.generateTraceFromBoatPosition(Double.parseDouble(localization[0]), Double.parseDouble(localization[1]));
                    } else {
                        osmMap.setCurrentBoatPositionWhileRunning(Double.parseDouble(localization[0]), Double.parseDouble(localization[1]));
                    }
                }
            });
        }
    }

    private void setLightPowerLabel() {
        Platform.runLater(() -> {
            lightPower.setText(lighting.getPower() + "%");
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

    private boolean checkCorrectlyReceivedData(String[] receivedData) {
        return receivedData.length == 2 && (Integer.parseInt(receivedData[0]) == FROM_BOAT_LIGHTING_MESSAGE ||
                Integer.parseInt(receivedData[0]) == FROM_BOAT_GPS_MESSAGE ||
                Integer.parseInt(receivedData[0]) == FROM_BOAT_BOAT_IS_SWIMMING_BY_WAYPOINTS ||
                Integer.parseInt(receivedData[0]) == FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS ||
                Integer.parseInt(receivedData[0]) == FROM_BOAT_BOAT_MANUALLY_FINISHED_SWIMMING_BY_WAYPOINTS ||
                Integer.parseInt(receivedData[0]) == FROM_BOAT_GPS_COURSE_MESSAGE ||
                Integer.parseInt(receivedData[0]) == FROM_BOAT_SENSOR_COURSE_MESSAGE);
    }
}
