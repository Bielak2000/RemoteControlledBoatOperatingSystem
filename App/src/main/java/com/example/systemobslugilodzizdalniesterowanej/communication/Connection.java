package com.example.systemobslugilodzizdalniesterowanej.communication;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.AutonomicController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.BasicCourseAndGpsAlgorithm;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.KalmanFilterAlgorithm;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.LinearAndAngularSpeed;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.PositionAlgorithm;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Engines;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Flaps;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Lighting;
import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.example.systemobslugilodzizdalniesterowanej.controllers.ProgressDialogController;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.FXML_RESOURCES_PATH;
import static jssc.SerialPort.MASK_RXCHAR;

@Slf4j
public class Connection {

    private final static int MAX_COURSE_COUNT_IN_AUTONOMIC_STARTING_MODE = 5;
    private final static int COURSE_ACCURACY = 10;

    // MESSAGE TO BOAT
    private final static String FROM_APP_KEYBOARD_CONTROL_MODE_MARKING = "0";
    private final static String FROM_APP_MOVE_TO_AUTONOMIC_MODE = "1";
    private final static String FROM_APP_AUTONOMOUS_MODE_CONTROL = "2";
    private final static String FINISH_SWIMMING_BY_WAYPOINTS = "3";
    private final static String FROM_APP_INIT_CONNECTION = "4";

    // MESSAGE FROM BOAT
    private final static int FROM_BOAT_LIGHTING_MESSAGE = 0;
    private final static int FROM_BOAT_GPS_MESSAGE = 1;
    private final static int FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS = 2;

    private final static String BOAT_FINISHED_SWIMMING_INFORMATION = "Łódka dopłyneła do ostaniego waypointa, zmieniono tryb sterowania na tryb manualny. Jeśli chcesz ponownie wyznaczyć trasę wykonaj odpowiednie czynności jak poprzednio.";
    private final static String BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION = "Ręcznie przerwano pływanie łodzi po waypointach, zmieniono tryb sterowania na tryb manualny.";

    // TODO: do testow
    private final static int FROM_BOAT_GPS_COURSE_MESSAGE = 5;
    private final static int FROM_BOAT_SENSOR_COURSE_MESSAGE = 6;
    private final static int LINEAR_ACCELERATION_ANGULAR_SPEED_ASSIGN = 7;

    @Getter
    private Lock sendingValuesLock = new ReentrantLock();
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
    private Label designatedCourse;

    private Boolean networkStatus;
    private OSMMap osmMap;
    private Stage stage;
    private ToggleButton modeChooser;
    private PositionAlgorithm chosenAlgorithm;
    private LinearAndAngularSpeed oldLinearAndAngularSpeed = null;
    private LocalDateTime now = LocalDateTime.now();
    private String fileName;
    private Label expectedCourse;

    public Connection(Engines engines, Lighting lighting, Flaps flaps, Label connectionStatus, Label lightPower, Boolean networkStatus, OSMMap osmMap,
                      Stage stage, BoatModeController boatModeController, AutonomicController autonomicController,
                      Label gpsCourse, Label sensorCourse, ToggleButton modeChooser, PositionAlgorithm chosenAlgorithm, Label designatedCourse,
                      KalmanFilterAlgorithm kalmanFilterAlgorithm, Label expectedCourse) {
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
        this.autonomicController = autonomicController;
        this.gpsCourse = gpsCourse;
        this.sensorCourse = sensorCourse;
        this.modeChooser = modeChooser;
        this.chosenAlgorithm = chosenAlgorithm;
        this.basicCourseAndGpsAlgorithm = new BasicCourseAndGpsAlgorithm(expectedCourse);
        this.expectedCourse = expectedCourse;
        this.kalmanFilterAlgorithm = kalmanFilterAlgorithm;
        this.designatedCourse = designatedCourse;
        initCSV();
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

    public void sendInitConnection() {
        try {
            if (boatModeController.getBoatMode() == BoatMode.KEYBOARD_CONTROL) {
                String sentInfo = (FROM_APP_INIT_CONNECTION + "_" + chosenAlgorithm.getMaking() + "_");
                serialPort.writeString(sentInfo);
                log.info("Sent init connection: {}", sentInfo);
            }
        } catch (SerialPortException e) {
            log.error("Error while sending message: {}", e.getMessage());
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
                if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_RUNNING || boatModeController.getBoatMode() == BoatMode.AUTONOMIC_STARTING) {
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

    public void designateAndSendEnginesPowerByAutonomicController() throws IOException {
        LinearAndAngularSpeed linearAndAngularSpeed = autonomicController.designateEnginesPower();
        if (chosenAlgorithm.equals(PositionAlgorithm.KALMAN_FILTER)) {
            if (kalmanFilterAlgorithm.getNextWaypoint() != null && !kalmanFilterAlgorithm.getNextWaypoint().equals(osmMap.getNextWaypointOnTheRoad())) {
                kalmanFilterAlgorithm.setStartWaypoint(kalmanFilterAlgorithm.getNextWaypoint());
            }
            kalmanFilterAlgorithm.setNextWaypoint(osmMap.getNextWaypointOnTheRoad());
        }
        if (linearAndAngularSpeed != null) {
            if (linearAndAngularSpeed.equals(oldLinearAndAngularSpeed)) {
                log.info("Designated new linear and angular speed but not changed ...");
            } else {
                sendEnginesPowerInAutonomicMode(linearAndAngularSpeed);
                oldLinearAndAngularSpeed = linearAndAngularSpeed;
            }
        } else {
            privateSetProgressDialogController("Koniec trasy", "Łódź osiągneła cel, zatrzymywanie łodzi ...");
            if (chosenAlgorithm.equals(PositionAlgorithm.KALMAN_FILTER)) {
                kalmanFilterAlgorithm.setNextWaypoint(null);
            }
            autonomicController.setManuallyFinishSwimming(false);
            sendStopSwimmingInfo();
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
                log.info("Received localization");
                String[] localization = {"", ""};
                if (!array[1].equals("-1") && !array[1].equals("INVALID")) {
                    localization = array[1].split(",");
                    log.info("New localization: {}", array[1]);
                }
                if (localization.length == 2 && boatModeController.getBoatMode() != BoatMode.AUTONOMIC_STARTING) {
                    sendingValuesLock.lock();
                    setBoatPositionOnMap(localization);
                    sendingValuesLock.unlock();
                }
                break;
            case FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS:
                log.info("Received boat finished swimming by waypoints.");
                sendingValuesLock.lock();
                if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                    kalmanFilterAlgorithm.getLock().lock();
                }
                Platform.runLater(() -> {
                    if (progressDialogController != null) {
                        progressDialogController.closeProgressDialogController();
                        this.progressDialogController = null;
                    }
                    osmMap.clearCurrentBoatPositionAfterFinishedLastWaypoint();
                    if (autonomicController.isManuallyFinishSwimming()) {
                        showInformationDialog("Przerwano pływanie łodzi", BOAT_MANUALLY_FINISHED_SWIMMING_INFORMATION, 500);
                    } else {
                        showInformationDialog("Łódka osiągneła punkt docelowy", BOAT_FINISHED_SWIMMING_INFORMATION, 700);
                    }
                    autonomicController.setManuallyFinishSwimming(true);
                });
                boatModeController.setBoatMode(BoatMode.KEYBOARD_CONTROL);
                engines.setTemp(false);
                osmMap.setNextWaypointOnTheRoad(null);
                osmMap.setWaypointIndex(0);
                boatModeController.setBoatMode(BoatMode.KEYBOARD_CONTROL);
                modeChooser.setSelected(false);
                if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                    kalmanFilterAlgorithm.getLock().unlock();
                }
                sendingValuesLock.unlock();
                break;
            case FROM_BOAT_GPS_COURSE_MESSAGE:
                log.info("Received course from GPS");
                Platform.runLater(() -> {
                    this.gpsCourse.setText(array[1]);
                });
                sendingValuesLock.lock();
                if (chosenAlgorithm == PositionAlgorithm.ONLY_GPS) {
                    osmMap.setCurrentCourse(Double.parseDouble(array[1]));
                    Utils.saveDesignatedValueToCSVFile(fileName, osmMap.getCurrentBoatPosition(), osmMap.getCurrentCourse(), expectedCourse.getText(), osmMap.getNextWaypointOnTheRoad(), osmMap.getStartWaypoint());
                } else if (chosenAlgorithm == PositionAlgorithm.BASIC_ALGORITHM) {
                    basicCourseAndGpsAlgorithm.setGpsCourseIfCorrectData(Double.parseDouble(array[1]));
                    Double designatedCourseFromBasicAlgorithm = basicCourseAndGpsAlgorithm.designateCurrentCourse();
                    basicCourseAndGpsAlgorithm.saveDesignatedValueToCSVFile(osmMap.getCurrentBoatPosition(), designatedCourseFromBasicAlgorithm, osmMap.getNextWaypointOnTheRoad(), osmMap.getStartWaypoint());
                    if (designatedCourseFromBasicAlgorithm != null) {
                        Platform.runLater(() -> {
                            designatedCourse.setText(String.format("%.2f", designatedCourseFromBasicAlgorithm));
                            osmMap.setCurrentCourse(designatedCourseFromBasicAlgorithm);
                        });
                    }
                } else if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                    kalmanFilterAlgorithm.getLock().lock();
                    double gpsCourse = Double.parseDouble(array[1]);
                    kalmanFilterAlgorithm.setGpsCourse(gpsCourse);
                    if (gpsCourse != 0) {
                        kalmanFilterAlgorithm.setFoundGpsCourse(true);
                    }
                    kalmanFilterAlgorithm.getLock().unlock();
                }
                sendingValuesLock.unlock();
                break;
            case FROM_BOAT_SENSOR_COURSE_MESSAGE:
                log.info("Received course from SENSOR");
                sendingValuesLock.lock();
                Platform.runLater(() -> {
                    this.sensorCourse.setText(array[1]);
                });

                if (chosenAlgorithm == PositionAlgorithm.GPS_AND_SENSOR) {
                    osmMap.setCurrentCourse(Double.parseDouble(array[1]));
                    Utils.saveDesignatedValueToCSVFile(fileName, osmMap.getCurrentBoatPosition(), osmMap.getCurrentCourse(), expectedCourse.getText(), osmMap.getNextWaypointOnTheRoad(), osmMap.getStartWaypoint());
                } else if (chosenAlgorithm == PositionAlgorithm.BASIC_ALGORITHM) {
                    basicCourseAndGpsAlgorithm.setSensorCourseIfCorrectData(Double.parseDouble(array[1]));
                    Double designatedCourseFromBasicAlgorithm = basicCourseAndGpsAlgorithm.designateCurrentCourse();
                    basicCourseAndGpsAlgorithm.saveDesignatedValueToCSVFile(osmMap.getCurrentBoatPosition(), designatedCourseFromBasicAlgorithm, osmMap.getNextWaypointOnTheRoad(), osmMap.getStartWaypoint());
                    if (designatedCourseFromBasicAlgorithm != null) {
                        Platform.runLater(() -> {
                            designatedCourse.setText(String.format("%.2f", designatedCourseFromBasicAlgorithm));
                        });
                        osmMap.setCurrentCourse(designatedCourseFromBasicAlgorithm);
                    }
                } else if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                    kalmanFilterAlgorithm.getLock().lock();
                    kalmanFilterAlgorithm.setSensorCourse(Double.parseDouble(array[1]));
                    kalmanFilterAlgorithm.getLock().unlock();
                }

                if (boatModeController.getBoatMode() == BoatMode.AUTONOMIC_STARTING && !autonomicController.isStopRotating()) {
                    autonomicController.incrementCourseCount();
                    if (autonomicController.getCourseCount() > MAX_COURSE_COUNT_IN_AUTONOMIC_STARTING_MODE) {
                        if (Math.abs(autonomicController.getCourseOnRotateStart() - Double.valueOf(array[1])) <= COURSE_ACCURACY) {
                            autonomicController.setStopRotating(true);
                        }
                    }
                }
                sendingValuesLock.unlock();
                break;
            case LINEAR_ACCELERATION_ANGULAR_SPEED_ASSIGN:
                log.info("Received linear acceleration and angular speed from SENSOR");
                if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                    String[] linearAccelerationAndAngularSpeed = array[1].split(",");
                    if (linearAccelerationAndAngularSpeed.length == 3) {
                        try {
                            kalmanFilterAlgorithm.getLock().lock();
                            kalmanFilterAlgorithm.setAccelerationX(Double.parseDouble(linearAccelerationAndAngularSpeed[0]));
                            kalmanFilterAlgorithm.setAccelerationY(Double.parseDouble(linearAccelerationAndAngularSpeed[1]));
                            kalmanFilterAlgorithm.setAngularSpeed(Double.parseDouble(linearAccelerationAndAngularSpeed[2]));
                            kalmanFilterAlgorithm.getLock().unlock();
                        } catch (NumberFormatException ex) {
                            log.error("Wrong linearAccelerationAndAngularSpeed values ...");
                        }
                    } else {
                        log.error("Wrong linearAccelerationAndAngularSpeed values ...");
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
                    if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
                        kalmanFilterAlgorithm.getLock().lock();
                        kalmanFilterAlgorithm.setGpsLocalization(new Coordinate(Double.parseDouble(localization[0]), Double.parseDouble(localization[1])));
                        kalmanFilterAlgorithm.getLock().unlock();
                    } else {
                        if (currentBoatMode != BoatMode.AUTONOMIC_STARTING && currentBoatMode != BoatMode.AUTONOMIC_RUNNING) {
                            osmMap.generateTraceFromBoatPosition(Double.parseDouble(localization[0]), Double.parseDouble(localization[1]));
                        } else {
                            osmMap.setCurrentBoatPositionWhileRunning(Double.parseDouble(localization[0]), Double.parseDouble(localization[1]));
                        }
                        if (chosenAlgorithm == PositionAlgorithm.BASIC_ALGORITHM) {
                            basicCourseAndGpsAlgorithm.saveDesignatedValueToCSVFile(new Coordinate(Double.parseDouble(localization[0]), Double.parseDouble(localization[1])), null, osmMap.getNextWaypointOnTheRoad(), osmMap.getStartWaypoint());
                        } else {
                            Utils.saveDesignatedValueToCSVFile(fileName, new Coordinate(Double.parseDouble(localization[0]), Double.parseDouble(localization[1])), osmMap.getCurrentCourse(), expectedCourse.getText(), osmMap.getNextWaypointOnTheRoad(), osmMap.getStartWaypoint());
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
        alert.show();
    }

    private boolean checkCorrectlyReceivedData(String[] receivedData) {
        if (receivedData.length == 1) {
            try {
                return Integer.parseInt(receivedData[0]) == FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS;
            } catch (NumberFormatException ex) {
                return false;
            }
        } else if (receivedData.length == 2) {
            try {
                return Integer.parseInt(receivedData[0]) == FROM_BOAT_LIGHTING_MESSAGE ||
                        Integer.parseInt(receivedData[0]) == FROM_BOAT_GPS_MESSAGE ||
                        Integer.parseInt(receivedData[0]) == FROM_BOAT_BOAT_FINISHED_SWIMMING_BY_WAYPOINTS ||
                        Integer.parseInt(receivedData[0]) == FROM_BOAT_GPS_COURSE_MESSAGE ||
                        Integer.parseInt(receivedData[0]) == FROM_BOAT_SENSOR_COURSE_MESSAGE ||
                        Integer.parseInt(receivedData[0]) == LINEAR_ACCELERATION_ANGULAR_SPEED_ASSIGN;
            } catch (NumberFormatException ex) {
                return false;
            }
        } else {
            return false;
        }
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

    private void initCSV() {
        if (chosenAlgorithm == PositionAlgorithm.BASIC_ALGORITHM) {
            basicCourseAndGpsAlgorithm.saveInitValToCsv();
        } else if (chosenAlgorithm == PositionAlgorithm.GPS_AND_SENSOR) {
            fileName = "gps-and-sensor-" + now.format(Utils.formatter);
            Utils.saveInitValToCsvForNotBasicAndKalmanAlgorithm(fileName);
        } else if (chosenAlgorithm == PositionAlgorithm.ONLY_GPS) {
            fileName = "gps-" + now.format(Utils.formatter);
            Utils.saveInitValToCsvForNotBasicAndKalmanAlgorithm(fileName);
        }
    }
}
