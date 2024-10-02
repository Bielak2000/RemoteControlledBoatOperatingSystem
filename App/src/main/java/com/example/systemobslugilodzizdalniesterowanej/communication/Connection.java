package com.example.systemobslugilodzizdalniesterowanej.communication;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.AutonomicController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.BasicCourseAndGpsAlgorithm;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.KalmanFilterAlgorithm;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.LinearAndAngularSpeed;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Engines;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Flaps;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Lighting;
import com.example.systemobslugilodzizdalniesterowanej.controllers.ProgressDialogController;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import com.example.systemobslugilodzizdalniesterowanej.positionalgorithm.PositionAlgorithm;
import com.sothawo.mapjfx.Coordinate;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.FXML_RESOURCES_PATH;
import static jssc.SerialPort.MASK_RXCHAR;

@Slf4j
public class Connection {

    private final static int MAX_COURSE_COUNT_IN_AUTONOMIC_STARTING_MODE = 5;
    private final static int COURSE_ACCURACY = 5;

    // MESSAGE TO BOAT
    private final static String FROM_APP_KEYBOARD_CONTROL_MODE_MARKING = "0";
    private final static String FROM_APP_MOVE_TO_AUTONOMIC_MODE = "1";
    private final static String FROM_APP_AUTONOMOUS_MODE_CONTROL = "2";
    private final static String FINISH_SWIMMING_BY_WAYPOINTS = "3";

    // MESSAGE FROM BOAT
    private final static int FROM_BOAT_LIGHTING_MESSAGE = 0;
    private final static int FROM_BOAT_GPS_MESSAGE = 1;
    private final static int FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS = 2;

    private final static String BOAT_FINISHED_SWIMMING_INFORMATION = "Łódka dopłyneła do ostaniego waypointa, zmieniono tryb sterowania na tryb manualny. Jeśli chcesz ponownie wyznaczyć trasę wykonaj odpowiednie czynności jak poprzednio.";
    private final static String BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION = "Ręcznie przerwano pływanie łodzi po waypointach, zmieniono tryb sterowania na tryb manualny.";

    // TODO: do testow
    private final static int FROM_BOAT_GPS_COURSE_MESSAGE = 5;
    private final static int FROM_BOAT_SENSOR_COURSE_MESSAGE = 6;

    private BasicCourseAndGpsAlgorithm basicCourseAndGpsAlgorithm;
    private KalmanFilterAlgorithm kalmanFilterAlgorithm;
    private AutonomicController autonomicController;
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
    private ToggleButton modeChooser;
    private PositionAlgorithm chosenAlgorithm;

    public Connection(Engines engines, Lighting lighting, Flaps flaps, Label connectionStatus, Label lightPower, Boolean networkStatus, OSMMap osmMap,
                      Stage stage, BoatModeController boatModeController, Label runningBoatInformation, AutonomicController autonomicController,
                      Label gpsCourse, Label sensorCourse, ToggleButton modeChooser, PositionAlgorithm chosenAlgorithm) {
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
        this.runningBoatInformation = runningBoatInformation;
        this.autonomicController = autonomicController;
        this.gpsCourse = gpsCourse;
        this.sensorCourse = sensorCourse;
        this.modeChooser = modeChooser;
        this.chosenAlgorithm = chosenAlgorithm;
        this.basicCourseAndGpsAlgorithm = new BasicCourseAndGpsAlgorithm();
        this.kalmanFilterAlgorithm = new KalmanFilterAlgorithm();
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
                    } catch (SerialPortException | IOException ex) {
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
                        + String.valueOf((int) engines.getMotorLeft()) + "_" + String.valueOf((int) engines.getMotorRight()) + "_"
                        + String.valueOf((int) lighting.getPower()) + "_"
                        + String.valueOf((int) flaps.getFirstFlap()) + "_" + flaps.getSecondFlap() + "_");
                serialPort.writeString(sentInfo);
                log.info("Sent: {}", sentInfo);
            }
        } catch (SerialPortException e) {
            log.error("Error while sending message: {}", e.getMessage());
        }
    }

    public void sendEnginesPowerInAutonomicMode(LinearAndAngularSpeed linearAndAngularSpeed) {
        if (linearAndAngularSpeed != null) {
            engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
            try {
                if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING) {
                    String sentInfo = (FROM_APP_AUTONOMOUS_MODE_CONTROL + "_"
                            + String.valueOf((int) engines.getMotorLeft()) + "_" + String.valueOf((int) engines.getMotorRight()) + "_");
                    serialPort.writeString(sentInfo);
                    log.info("Sent: {}", sentInfo);
                }
            } catch (SerialPortException e) {
                log.error("Error while sending message: {}", e.getMessage());
            }
        }
    }

    public void sendStopSwimmingInfo() {
        try {
            String sentInfo = FINISH_SWIMMING_BY_WAYPOINTS;
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

    private void receivedMessageHandler(int messageSign, String[] array) throws IOException {
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
                    if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING) {
                        LinearAndAngularSpeed linearAndAngularSpeed = autonomicController.designateEnginesPower();
                        if (linearAndAngularSpeed != null) {
                            sendEnginesPowerInAutonomicMode(linearAndAngularSpeed);
                        } else {
                            privateSetProgressDialogController("Koniec trasy", "Łódź osiągneła cel, zatrzymywanie łodzi ...");
                            autonomicController.setManuallyFinishSwimming(false);
                            sendStopSwimmingInfo();
                        }
                    }
                }
                break;
            case FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS:
                Platform.runLater(() -> {
                    progressDialogController.closeProgressDialogController();
                    this.progressDialogController = null;
                    boatModeController.setBoatMode(BoatMode.KEYBOARD_CONTROL);
                    // TODO: przetestowac czy uda sie wyczyscic mape
                    osmMap.clearCurrentBoatPositionAfterFinishedLastWaypoint();
                    if (autonomicController.isManuallyFinishSwimming()) {
                        showInformationDialog("Przerwano pływanie łodzi", BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION, 500);
                    } else {
                        showInformationDialog("Łódka osiągneła punkt docelowy", BOAT_FINISHED_SWIMMING_INFORMATION, 700);
                    }
                    autonomicController.setManuallyFinishSwimming(true);
                });
                osmMap.setNextWaypointOnTheRoad(null);
                osmMap.setWaypointIndex(0);
                boatModeController.setBoatMode(BoatMode.KEYBOARD_CONTROL);
                modeChooser.setSelected(false);
                break;
            case FROM_BOAT_GPS_COURSE_MESSAGE:
                Platform.runLater(() -> {
                    this.gpsCourse.setText(array[1]);
                });

                if (chosenAlgorithm == PositionAlgorithm.ONLY_GPS) {
                    osmMap.setCurrentCourse(Double.parseDouble(array[1]));
                } else if (chosenAlgorithm == PositionAlgorithm.BASIC_ALGORITHM) {
                    basicCourseAndGpsAlgorithm.setGpsCourse(Double.parseDouble(array[1]));
                    osmMap.setCurrentCourse(basicCourseAndGpsAlgorithm.designateCurrentCourse());
                } else if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                    kalmanFilterAlgorithm.setGpsCourse(Double.parseDouble(array[1]));
                    kalmanFilterAlgorithm.designateCurrentCourseAndLocalization();
                    osmMap.setCurrentCourse(kalmanFilterAlgorithm.getCurrentCourse());
                    if (boatModeController.getBoatMode() != BoatMode.AUTONOMIC_STARTING && boatModeController.getBoatMode() != BoatMode.AUTONOMIC_RUNNING) {
                        osmMap.generateTraceFromBoatPosition(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                    } else {
                        osmMap.setCurrentBoatPositionWhileRunning(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                    }
                }

                if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING &&
                        (chosenAlgorithm == PositionAlgorithm.ONLY_GPS || chosenAlgorithm == PositionAlgorithm.BASIC_ALGORITHM || chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER)) {
                    LinearAndAngularSpeed linearAndAngularSpeed = autonomicController.designateEnginesPower();
                    sendEnginesPowerInAutonomicMode(linearAndAngularSpeed);
                }

                break;
            case FROM_BOAT_SENSOR_COURSE_MESSAGE:
                Platform.runLater(() -> {
                    this.sensorCourse.setText(array[1]);
                });

                if (chosenAlgorithm == PositionAlgorithm.GPS_AND_SENSOR) {
                    osmMap.setCurrentCourse(Double.parseDouble(array[1]));
                } else if (chosenAlgorithm == PositionAlgorithm.BASIC_ALGORITHM) {
                    basicCourseAndGpsAlgorithm.setSensorCourse(Double.parseDouble(array[1]));
                    osmMap.setCurrentCourse(basicCourseAndGpsAlgorithm.designateCurrentCourse());
                } else if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                    kalmanFilterAlgorithm.setSensorCourse(Double.parseDouble(array[1]));
                    kalmanFilterAlgorithm.designateCurrentCourseAndLocalization();
                    osmMap.setCurrentCourse(kalmanFilterAlgorithm.getCurrentCourse());
                    if (boatModeController.getBoatMode() != BoatMode.AUTONOMIC_STARTING && boatModeController.getBoatMode() != BoatMode.AUTONOMIC_RUNNING) {
                        osmMap.generateTraceFromBoatPosition(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                    } else {
                        osmMap.setCurrentBoatPositionWhileRunning(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                    }
                }

                if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING &&
                        (chosenAlgorithm == PositionAlgorithm.GPS_AND_SENSOR || chosenAlgorithm == PositionAlgorithm.BASIC_ALGORITHM || chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER)) {
                    LinearAndAngularSpeed linearAndAngularSpeed = autonomicController.designateEnginesPower();
                    sendEnginesPowerInAutonomicMode(linearAndAngularSpeed);
                } else if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_STARTING && !autonomicController.isStopRotating()) {
                    autonomicController.incrementCourseCount();
                    if (autonomicController.getCourseCount() > MAX_COURSE_COUNT_IN_AUTONOMIC_STARTING_MODE) {
                        if (Math.abs(autonomicController.getCourseOnRotateStart() - Double.valueOf(array[1])) <= COURSE_ACCURACY) {
                            autonomicController.setStopRotating(true);
                        }
                    }
                }

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
                        if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                            kalmanFilterAlgorithm.setLocalization(new Coordinate(Double.parseDouble(localization[0]), Double.parseDouble(localization[1])));
                            kalmanFilterAlgorithm.designateCurrentCourseAndLocalization();
                            osmMap.generateTraceFromBoatPosition(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                        } else {
                            osmMap.generateTraceFromBoatPosition(Double.parseDouble(localization[0]), Double.parseDouble(localization[1]));
                        }
                    } else {
                        if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                            kalmanFilterAlgorithm.setLocalization(new Coordinate(Double.parseDouble(localization[0]), Double.parseDouble(localization[1])));
                            kalmanFilterAlgorithm.designateCurrentCourseAndLocalization();
                            osmMap.setCurrentBoatPositionWhileRunning(kalmanFilterAlgorithm.getCurrentLocalization().getLatitude(), kalmanFilterAlgorithm.getCurrentLocalization().getLongitude());
                        } else {
                            osmMap.setCurrentBoatPositionWhileRunning(Double.parseDouble(localization[0]), Double.parseDouble(localization[1]));
                        }
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
                Integer.parseInt(receivedData[0]) == FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS ||
                Integer.parseInt(receivedData[0]) == FROM_BOAT_GPS_COURSE_MESSAGE ||
                Integer.parseInt(receivedData[0]) == FROM_BOAT_SENSOR_COURSE_MESSAGE);
    }

    private void privateSetProgressDialogController(String title, String content) throws IOException {
        Stage stage1 = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_RESOURCES_PATH + "progress-dialog.fxml"));
        progressDialogController = new ProgressDialogController(stage1);
        fxmlLoader.setController(progressDialogController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage1.setScene(scene);
        progressDialogController.setDescriptions(title, content);
        stage1.show();
    }
}
