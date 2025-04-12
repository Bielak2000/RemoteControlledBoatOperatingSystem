package com.example.systemobslugilodzizdalniesterowanej.controllers;

import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatMode;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.BoatModeController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.AutonomicControlExecute;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.AutonomicController;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.KalmanFilterAlgorithm;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.PositionAlgorithm;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Engines;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Flaps;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Lighting;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.keyboardcontrol.KeyboardHandler;
import com.example.systemobslugilodzizdalniesterowanej.communication.Connection;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import com.sothawo.mapjfx.MapView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.systemobslugilodzizdalniesterowanej.common.Utils.FXML_RESOURCES_PATH;

@Slf4j
public class SystemController implements Initializable {
    KalmanFilterAlgorithm kalmanFilterAlgorithm;
    AutonomicControlExecute autonomicControlExecute;
    ExecutorService executorService;
    AutonomicController autonomicController;
    Stage stage;
    Boolean networkStatus;
    OSMMap osmMap;
    BoatModeController boatModeController;
    KeyboardHandler keyboardHandler;
    Connection connection;
    String chosenPort;
    Lighting lighting = new Lighting();
    Engines engines = new Engines();
    Flaps flaps = new Flaps();
    String chosenSystem;
    PositionAlgorithm chosenAlgorithm;

    public Stage getStage() {
        return stage;
    }

    public SystemController(Stage stage, String chosenPort, String chosenSystem, PositionAlgorithm chosenAlgorithm) {
        this.stage = stage;
        this.chosenPort = chosenPort;
        this.chosenSystem = chosenSystem;
        this.executorService = Executors.newFixedThreadPool(1);
        this.chosenAlgorithm = chosenAlgorithm;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.boatModeController = BoatModeController.getInstance(leftFlap, lightDown, lightUp, moveDown, moveLeft, moveRight,
                moveUp, rightFlap, lightingText, flapsText, startSwimming, clearTrace, modeChooser, exit, runningBoatInformation, stopSwimmingButton,
                gpsCourse, expectedCourse, sensorCourse, gpsCourseText, sensorCourseText, expectedCourseText, designatedCourse, designatedCourseText);
        try {
            checkConnectionWithInternet();
            osmMap = new OSMMap(mapView, boatModeController, expectedCourse);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        if (chosenAlgorithm == PositionAlgorithm.KALMAN_FILTER) {
            this.kalmanFilterAlgorithm = new KalmanFilterAlgorithm(expectedCourse);
            this.kalmanFilterAlgorithm.initializeKalmanFilter();
        }
        this.autonomicController = new AutonomicController(osmMap);
        this.connection = new Connection(engines, lighting, flaps, connectionStatus, lightPower, networkStatus, osmMap, stage,
                boatModeController, autonomicController, gpsCourse, sensorCourse, modeChooser, chosenAlgorithm, designatedCourse, kalmanFilterAlgorithm, expectedCourse);
        connection.connect(chosenPort, chosenSystem);
        networkStatus = false;
        lightPower.setText("0%");
        this.choosenAlgorithmText.setText(chosenAlgorithm.getDescription());
        exit.setCancelButton(true);
        exit.setFocusTraversable(false);
        this.autonomicControlExecute = new AutonomicControlExecute(this.boatModeController, this.connection, this.kalmanFilterAlgorithm, this.osmMap, this.designatedCourse, this.chosenAlgorithm);
        this.autonomicControlExecute.start();
        this.connection.sendInitConnection();
    }

    public void initializeKeyboardHandler() {
        this.keyboardHandler = new KeyboardHandler(stage.getScene(), connection, boatModeController,
                moveUp, moveDown, moveLeft, moveRight, leftFlap, rightFlap, lightDown, lightUp, engines, lighting, flaps);
        keyboardHandler.keyboardHandler();
    }

    @FXML
    private Label connectionStatus;

    @FXML
    private Label runningBoatInformation;

    @FXML
    private Label networkConnection;

    @FXML
    private Label lightingText;

    @FXML
    private Label flapsText;

    @FXML
    private ToggleButton modeChooser;

    @FXML
    private Button startSwimming;

    @FXML
    private MapView mapView;

    @FXML
    private Button leftFlap;

    @FXML
    private Button lightDown;

    @FXML
    private Label lightPower;

    @FXML
    private Button lightUp;

    @FXML
    private Button moveDown;

    @FXML
    private Button moveLeft;

    @FXML
    private Button moveRight;

    @FXML
    private Button moveUp;

    @FXML
    private Button rightFlap;

    @FXML
    private Button exit;

    @FXML
    private Button clearTrace;

    @FXML
    private Button stopSwimmingButton;

    @FXML
    private CheckBox mapOsmCheckBox;
    @FXML
    private Label choosenAlgorithmText;

    // TODO: w celu testach
    @FXML
    private Label gpsCourse;

    @FXML
    private Label expectedCourse;

    @FXML
    private Label sensorCourse;

    @FXML
    private Label gpsCourseText;

    @FXML
    private Label expectedCourseText;

    @FXML
    private Label sensorCourseText;
    // TODO: koniec testow

    @FXML
    private Label designatedCourse;

    @FXML
    private Label designatedCourseText;

    public Label getNetworkConnection() {
        return networkConnection;
    }

    @FXML
    void closeApplication(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_RESOURCES_PATH + "dialog-window.fxml"));
        Stage dialogStage = new Stage();
        ExitDialogController exitDialogController = new ExitDialogController(stage, dialogStage);
        fxmlLoader.setController(exitDialogController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        dialogStage.setTitle("Zamknij aplikacje");
        dialogStage.setScene(scene);
        dialogStage.show();
    }

    @FXML
    void clearTrace(ActionEvent event) {
        osmMap.clearMap();
    }

    @FXML
    void changeMode(ActionEvent event) throws IOException {
        if (modeChooser.isSelected()) {
            if (osmMap.getFoundBoatPosition()) {
//            if (true) {
                keyboardHandler.stopBoat();
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(200);
                            connection.sendMoveToAutonomicInfo();
                            changeBoatMode(BoatMode.AUTONOMIC);
                        } catch (InterruptedException e) {
                            log.error("Error while async sending changed boat mode and waypoints: {}", e.getMessage());
                        }
                    }
                });
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Brak danych");
                alert.setHeaderText("Nie wyznaczono aktualnego położenia łódki, spróbuj ponownie po pobraniu lokalizacji.");
                alert.showAndWait();
                modeChooser.setSelected(false);
            }
        } else {
            ProgressDialogController progressDialogController = manuallyStopSwimmingProgressDialog();
            connection.setProgressDialogController(progressDialogController);
            connection.sendStopSwimmingInfo();
        }
        modeChooser.getScene().getRoot().requestFocus();
    }

    @FXML
    void startSwimming(ActionEvent event) throws IOException {
        if (osmMap.getFoundBoatPosition() && osmMap.designatedWaypoints() && osmMap.getCurrentCourse() != null) {
//        if (true) {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_RESOURCES_PATH + "start-swimming-dialog.fxml"));
            Stage mainStage = new Stage();
            StartSwimmingDialogController startSwimmingDialogController = new StartSwimmingDialogController(chosenAlgorithm, mainStage, boatModeController, connection, osmMap, autonomicController, runningBoatInformation);
            fxmlLoader.setController(startSwimmingDialogController);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            mainStage.setScene(scene);
            mainStage.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Brak danych");
            alert.setHeaderText("Nie wyznaczono pozycji docelowej łódki, jej aktualnego położenia lub aktualnego kursu.");
            alert.showAndWait();
        }
    }

    @FXML
    void stopSwimming(ActionEvent event) throws IOException {
        ProgressDialogController progressDialogController = manuallyStopSwimmingProgressDialog();
        connection.setProgressDialogController(progressDialogController);
        connection.sendStopSwimmingInfo();
        changeBoatMode(BoatMode.KEYBOARD_CONTROL);
        modeChooser.setSelected(false);
    }

    @FXML
    void changeToOsmMap(ActionEvent event) {
        if (mapOsmCheckBox.isSelected()) {
            osmMap.changeMapTypeToOSM();
        } else {
            osmMap.changeMapTypeToWMSMap();
        }
    }

    public void checkConnectionWithInternet() throws InterruptedException, IOException {
        try {
            URL url1 = new URL("https://www.geeksforgeeks.org/");
            URLConnection connection = url1.openConnection();
            connection.connect();
            networkConnection.setText("Polaczono z internetem!");
            networkStatus = true;
        } catch (Exception e) {
            modeChooser.setDisable(true);
            mapOsmCheckBox.setDisable(true);
            dialogNotConnect("Brak internetu", "Aplikacja nie może połączyć się z internetem! Brak możliwości przejścia w tryb autonomiczny.");
            getNetworkConnection().setTextFill(Color.color(1, 0, 0));
            getNetworkConnection().setText("Brak polaczenia z internetem! Brak lokalizacji!");
        }
    }

    private void changeBoatMode(BoatMode boatMode) {
        this.boatModeController.setBoatMode(boatMode);
        if (this.boatModeController.getBoatMode() == BoatMode.KEYBOARD_CONTROL) {
            osmMap.removeAllMarkersAndLinesWithoutBoatPosition();
        }
        log.info("Change to {} boat mode.", boatMode.name());
    }

    private void dialogNotConnect(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(text);
        alert.getDialogPane().setMaxWidth(500);
        alert.showAndWait();
    }

    private ProgressDialogController manuallyStopSwimmingProgressDialog() throws IOException {
        Stage stage1 = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_RESOURCES_PATH + "progress-dialog.fxml"));
        ProgressDialogController progressDialogController = new ProgressDialogController(stage1);
        fxmlLoader.setController(progressDialogController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage1.setScene(scene);
        progressDialogController.setDescriptions("Zatrzymywanie łodzi", "Trwa zatrzymywanie łodzi, proszę o cierpliowść ...");
        stage1.show();
        root.requestFocus();
        return progressDialogController;
    }
}
